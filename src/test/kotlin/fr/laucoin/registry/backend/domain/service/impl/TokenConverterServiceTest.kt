package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_BLOCKED_ACCOUNT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_IMPERSONATED_ACCOUNT
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.VEHICLE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.test.ModelExt.projectId
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.LOCKED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


class TokenConverterServiceTest {
    private val roleService: IRoleService = mock()
    private val userService: IUserService = mock()
    private val profileRepository: IProjectProfileModelRepository = mock()
    private val service = TokenConverterService(
        userService, profileRepository, roleService, USER_ID_KEY, EMAIL_KEY, FIRST_NAME_KEY, LAST_NAME_KEY
    )

    companion object {
        private const val USER_ID_KEY = "sub"
        private const val EMAIL_KEY = "email"
        private const val FIRST_NAME_KEY = "given_name"
        private const val LAST_NAME_KEY = "family_name"

        @JvmStatic
        fun `Should convert throw JwtConversionException when jwt does not have email or user id`(): Stream<Arguments> = Stream.of(
            Arguments.of(true, false),
            Arguments.of(false, true),
            Arguments.of(false, false),
        )

        @JvmStatic
        fun `Should convert throw JwtConversionException when user is not visible or purged`(): Stream<Arguments> = Stream.of(
            Arguments.of(false, false, LOCKED, AUTH_BLOCKED_ACCOUNT),
            Arguments.of(true, true, CONFLICT, AUTH_IMPERSONATED_ACCOUNT),
            Arguments.of(false, true, LOCKED, AUTH_BLOCKED_ACCOUNT),
        )

        @JvmStatic
        fun `Should convert return Authentication`(): Stream<Arguments> {
            val currentUser = CurrentUserModel().apply {
                email = "john.doe@test.com"
                firstName = "John"
                lastName = "DOE"
            }
            return Stream.of(
                Arguments.of(currentUser, currentUser, null, 0, 1),
                Arguments.of(null, currentUser, emptyList<ProjectOptionEnum>(), 1, 0),
                Arguments.of(currentUser, currentUser, listOf(VEHICLE), 0, 1),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should convert throw JwtConversionException when jwt does not have email or user id`(
        hasUserId: Boolean,
        hasEmail: Boolean,
    ) {
        // Arrange
        val jwt = mock<Jwt>()
        whenever(jwt.hasClaim(USER_ID_KEY)).thenReturn(hasUserId)
        whenever(jwt.hasClaim(EMAIL_KEY)).thenReturn(hasEmail)

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.convert(jwt).block()
        }) as JwtConversionException

        // Assert
        assertEquals(UNAUTHORIZED, result.status)
        assertEquals(AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN, result.message)

        verifyNoInteractions(roleService)
        verifyNoInteractions(userService)
        verifyNoInteractions(profileRepository)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should convert throw JwtConversionException when user is not visible or purged`(
        userVisible: Boolean,
        userPurged: Boolean,
        expectedStatus: HttpStatus,
        expectedMessage: String,
    ) {
        // Arrange
        val jwt = mock<Jwt>()
        val userId = UUID.randomUUID()
        whenever(jwt.claims).thenReturn(
            mapOf(
                USER_ID_KEY to userId.toString(),
                EMAIL_KEY to "john.doe@test.com",
                FIRST_NAME_KEY to "John",
                LAST_NAME_KEY to "DOE",
            )
        )
        whenever(jwt.hasClaim(any())).thenCallRealMethod()
        whenever(jwt.getClaimAsString(any())).thenCallRealMethod()

        val currentUser = CurrentUserModel().apply {
            visible = userVisible
            purged = userPurged
        }
        whenever(userService.findUserByOidcId(any(), anyOrNull())).thenReturn(Mono.just(currentUser))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.convert(jwt).block()
        }) as JwtConversionException

        // Assert
        assertEquals(expectedStatus, result.status)
        assertEquals(expectedMessage, result.message)

        verifyNoInteractions(roleService)
        verify(userService).findUserByOidcId(userId, visibilitySearched = null)
        verifyNoInteractions(profileRepository)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should convert return Authentication`(
        databaseUser: CurrentUserModel?,
        currentUser: CurrentUserModel,
        projectOptions: List<ProjectOptionEnum>?,
        expectedUserCreation: Int,
        expectedUserUpdate: Int,
    ) {
        // Arrange
        val jwt = mock<Jwt>()
        val userId = UUID.randomUUID()
        val userOidcId = UUID.randomUUID()
        whenever(jwt.claims).thenReturn(
            mapOf(
                USER_ID_KEY to userOidcId.toString(),
                EMAIL_KEY to currentUser.email,
                FIRST_NAME_KEY to currentUser.firstName,
                LAST_NAME_KEY to currentUser.lastName,
            )
        )
        whenever(jwt.hasClaim(any())).thenCallRealMethod()
        whenever(jwt.getClaimAsString(any())).thenCallRealMethod()

        whenever(userService.findUserByOidcId(any(), anyOrNull())).thenReturn(Mono.justOrEmpty(databaseUser?.apply {
            visible = true
            purged = false
        }))

        val userRole = "USER_ROLE"
        whenever(userService.createUser(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Mono.just(currentUser.apply { id = userId; oidcId = userOidcId; role = userRole }))
        whenever(userService.updateUserIfPersonalDataChanged(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Mono.just(currentUser.apply { id = userId; oidcId = userOidcId; role = userRole }))

        val projectRole = "PROJECT_ROLE"
        val role = ProjectProfileRoleModel(
            projectId,
            projectOptions = projectOptions,
            role = projectRole
        )

        whenever(profileRepository.findProjectProfilesRolesByUserId(any())).thenReturn(Flux.just(role))
        whenever(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(emptyList())
        whenever(roleService.getAuthoritiesByProjectRole(any(), any(), anyOrNull())).thenReturn(emptyList())

        // Act
        service.convert(jwt).block()

        // Assert
        verify(userService).findUserByOidcId(userOidcId, visibilitySearched = null)
        verify(userService, times(expectedUserCreation)).createUser(
            userOidcId,
            currentUser.email !!,
            currentUser.firstName,
            currentUser.lastName
        )
        verify(userService, times(expectedUserUpdate)).updateUserIfPersonalDataChanged(
            any(),
            eq(currentUser.email !!),
            eq(currentUser.firstName),
            eq(currentUser.lastName),
        )
        verify(profileRepository).findProjectProfilesRolesByUserId(userId)
        verify(roleService).getAuthoritiesByUserRole(userRole)
        verify(roleService).getAuthoritiesByProjectRole(eq(projectRole), eq(projectId), anyOrNull())
    }
}
