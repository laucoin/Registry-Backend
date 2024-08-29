package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_BLOCKED_ACCOUNT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_IMPERSONATED_ACCOUNT
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.RegistryExceptionModel
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.domain.service.IUserService
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.lenient
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


class TokenConverterServiceTest {
    private val roleService: IRoleService = mock()
    private val userService: IUserService = mock()
    private val profileService: IUserEventProfileService = mock()

    private val service = TokenConverterService(
        userService = userService,
        profileService = profileService,
        roleService = roleService,
        userIdKey = USER_ID_KEY,
        emailKey = EMAIL_KEY,
        firstNameKey = FIRST_NAME_KEY,
        lastNameKey = LAST_NAME_KEY,
    )

    companion object {
        private const val USER_ID_KEY = "sub"
        private const val EMAIL_KEY = "email"
        private const val FIRST_NAME_KEY = "given_name"
        private const val LAST_NAME_KEY = "family_name"

        @JvmStatic
        fun `Should convert throw RegistryExceptionModel when jwt does not have email or user id`(): Stream<Arguments> = Stream.of(
            Arguments.of(true, false),
            Arguments.of(false, true),
            Arguments.of(false, false),
        )

        @JvmStatic
        fun `Should convert throw RegistryExceptionModel when user is not visible or purged`(): Stream<Arguments> = Stream.of(
            Arguments.of(false, false, AUTH_BLOCKED_ACCOUNT),
            Arguments.of(true, true, AUTH_IMPERSONATED_ACCOUNT),
            Arguments.of(false, true, AUTH_BLOCKED_ACCOUNT),
        )

        @JvmStatic
        fun `Should convert return Authentication`(): Stream<Arguments> {
            val currentUser = CurrentUserModel().apply {
                email = "john.doe@test.com"
                firstName = "John"
                lastName = "DOE"
            }
            return Stream.of(
                Arguments.of(null, currentUser, 1, 0),
                Arguments.of(currentUser, currentUser, 0, 1),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should convert throw RegistryExceptionModel when jwt does not have email or user id`(
        hasUserId: Boolean,
        hasEmail: Boolean,
    ) {
        // Arrange
        val jwt = mock<Jwt>()
        `when`(jwt.hasClaim(USER_ID_KEY)).thenReturn(hasUserId)
        `when`(jwt.hasClaim(EMAIL_KEY)).thenReturn(hasEmail)

        // Act
        val result = assertThrows(RegistryExceptionModel::class.java) {
            service.convert(jwt).block()
        }

        // Assert
        assertEquals(UNAUTHORIZED, result.status)
        assertEquals(AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN, result.message)

        verifyNoInteractions(roleService)
        verifyNoInteractions(userService)
        verifyNoInteractions(profileService)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should convert throw RegistryExceptionModel when user is not visible or purged`(
        userVisible: Boolean,
        userPurged: Boolean,
        expectedMessage: String,
    ) {
        // Arrange
        val jwt = mock<Jwt>()
        val userId = UUID.randomUUID()
        `when`(jwt.claims).thenReturn(
            mapOf(
                USER_ID_KEY to userId.toString(),
                EMAIL_KEY to "john.doe@test.com",
                FIRST_NAME_KEY to "John",
                LAST_NAME_KEY to "DOE",
            )
        )
        `when`(jwt.hasClaim(any())).thenCallRealMethod()
        `when`(jwt.getClaimAsString(any())).thenCallRealMethod()

        val currentUser = CurrentUserModel().apply {
            visible = userVisible
            purged = userPurged
        }
        `when`(userService.findUserByOidcId(any(), any())).thenReturn(Mono.just(currentUser))

        // Act
        val result = assertThrows(RegistryExceptionModel::class.java) {
            service.convert(jwt).block()
        }

        // Assert
        assertEquals(UNAUTHORIZED, result.status)
        assertEquals(expectedMessage, result.message)

        verifyNoInteractions(roleService)
        verify(userService, times(1)).findUserByOidcId(userId, onlyVisible = false)
        verifyNoInteractions(profileService)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should convert return Authentication`(
        databaseUser: CurrentUserModel?,
        currentUser: CurrentUserModel,
        expectedUserCreation: Int,
        expectedUserUpdate: Int,
    ) {
        // Arrange
        val jwt = mock<Jwt>()
        val userId = UUID.randomUUID()
        val userOidcId = UUID.randomUUID()
        `when`(jwt.claims).thenReturn(
            mapOf(
                USER_ID_KEY to userOidcId.toString(),
                EMAIL_KEY to currentUser.email,
                FIRST_NAME_KEY to currentUser.firstName,
                LAST_NAME_KEY to currentUser.lastName,
            )
        )
        `when`(jwt.hasClaim(any())).thenCallRealMethod()
        `when`(jwt.getClaimAsString(any())).thenCallRealMethod()

        `when`(userService.findUserByOidcId(any(), any())).thenReturn(Mono.justOrEmpty(databaseUser?.apply {
            visible = true
            purged = false
        }))

        val userRole = "USER_ROLE"
        lenient().`when`(userService.createUser(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Mono.just(currentUser.apply { id = userId; oidcId = userOidcId; role = userRole }))
        `when`(userService.updateUserIfPersonalDataChanged(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Mono.just(currentUser.apply { id = userId; oidcId = userOidcId; role = userRole }))

        val profileId = UUID.randomUUID()
        val eventRole = "EVENT_ROLE"
        val eventId = UUID.randomUUID()
        val profile = EventProfileModel().apply {
            id = profileId
            role = eventRole
            event = EventModel().apply { id = eventId }
        }

        `when`(profileService.findAllUserEventProfiles(any(), any(), anyOrNull())).thenReturn(Flux.just(profile))
        `when`(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(emptyList())
        `when`(roleService.getAuthoritiesByEventRole(any(), any())).thenReturn(emptyList())

        // Act
        service.convert(jwt).block()

        // Assert
        verify(userService, times(1)).findUserByOidcId(userOidcId, onlyVisible = false)
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
        verify(profileService, times(1)).findAllUserEventProfiles(
            userId,
            onlyUsable = true,
            status = ACCEPTED,
        )
        verify(roleService, times(1)).getAuthoritiesByUserRole(userRole)
        verify(roleService, times(1)).getAuthoritiesByEventRole(eventRole, eventId)
    }
}
