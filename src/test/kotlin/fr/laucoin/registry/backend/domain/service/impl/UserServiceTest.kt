package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_IMPERSONATE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.util.ReflectionTestUtils.setField
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserServiceTest {
    private val repository: IUserModelRepository = mock()
    private val preferencesService: IPreferencesService = mock()
    private val userEventProfileService: IUserEventProfileService = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val roleService: IRoleService = mock()
    private val service = UserService(repository, preferencesService, userEventProfileService, transactionalOperator, roleService)

    private val serviceAccountId = UUID.randomUUID()
    private val serviceAccount = CurrentUserModel().apply { id = serviceAccountId; role = USER_ROLE }

    companion object {
        @JvmStatic
        fun `Should updateUserIfPersonalDataChanged update and return User`(): Stream<Arguments> = Stream.of(
            Arguments.of(UserModel()),
            Arguments.of(UserModel().apply {
                firstName = "John"
                lastName = "DOE"
                email = "John.DOE@test.com"
            }),
        )

        @JvmStatic
        fun `Should updateUserRoleById update and return User`(): Stream<Arguments> = Stream.of(
            Arguments.of(1, USER_ROLE, 0),
            Arguments.of(0, USER_ROLE, 1),
            Arguments.of(null, USER_ROLE, 0),
            Arguments.of(1, null, 0),
            Arguments.of(0, null, 1),
            Arguments.of(null, null, 0),
        )

        @JvmStatic
        fun `Should updateUserRoleById throw RegistryException`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            return Stream.of(
                Arguments.of(
                    1,
                    listOf(USER_ROLE),
                    "OTHER_USER_ROLE",
                    UserModel().apply { id = uuid; role = USER_ROLE },
                    emptyArray<UserModel>(),
                    FORBIDDEN,
                    USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN,
                    0,
                    0,
                ),
                Arguments.of(
                    0,
                    emptyList<String>(),
                    "OTHER_USER_ROLE",
                    UserModel().apply { id = uuid; role = USER_ROLE },
                    arrayOf(UserModel().apply { id = uuid; role = USER_ROLE }),
                    NOT_FOUND,
                    NOT_FOUND_WITH_GIVEN_IDENTIFIER,
                    0,
                    0,
                ),
                Arguments.of(
                    0,
                    listOf(USER_ROLE),
                    USER_ROLE,
                    UserModel().apply { id = uuid; role = USER_ROLE },
                    arrayOf(UserModel().apply { id = uuid; role = USER_ROLE }),
                    FORBIDDEN,
                    USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE,
                    1,
                    1,
                ),
                Arguments.of(
                    0,
                    listOf(USER_ROLE),
                    USER_ROLE,
                    UserModel().apply { id = uuid },
                    arrayOf(UserModel().apply { id = uuid }),
                    FORBIDDEN,
                    USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE,
                    0,
                    1,
                ),
            )
        }
    }

    @BeforeEach
    fun setUp() {
        setField(service, "serviceAccount", serviceAccount)
    }

    @Test
    fun `Should onApplicationEvent call repository findServiceAccount`() {
        // Arrange
        val event: ContextRefreshedEvent = mock()
        val serviceAccount = CurrentUserModel()
        whenever(repository.findServiceAccount()).thenReturn(Mono.just(serviceAccount))

        // Act
        service.onApplicationEvent(event)

        // Assert
        verify(repository).findServiceAccount()
    }

    @Test
    fun `Should findUsersPage call repository findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = UserSearchParamModel()
        whenever(repository.findPage(any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findUsersPage(pageable, params).block()

        // Assert
        verify(repository).findPage(pageable, params)
    }

    @Test
    fun `Should findUserById call repository findById`() {
        // Arrange
        val user = UserModel()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(user))

        // Act
        service.findUserById(uuid, onlyVisible).block()

        // Assert
        verify(repository).findById(uuid, onlyVisible)
    }

    @Test
    fun `Should findUserById call repository findById throw on empty result`() {
        // Arrange
        val onlyVisible = true
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(UserModel().apply { id = serviceAccountId }))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.findUserById(serviceAccountId, onlyVisible).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findById(serviceAccountId, onlyVisible)
    }

    @Test
    fun `Should findUserByOidcId call repository findByOidcId`() {
        // Arrange
        val onlyVisible = true
        whenever(repository.findByOidcId(any(), anyOrNull())).thenReturn(Mono.just(currentUser()))

        // Act
        service.findUserByOidcId(currentUser().oidcId !!, onlyVisible).block()

        // Assert
        verify(repository).findByOidcId(currentUser().oidcId !!, onlyVisible)
    }

    @Test
    fun `Should serviceAccount return service account`() {
        // Act
        val result = service.serviceAccount()

        // Assert
        assertEquals(serviceAccount, result)
        verifyNoInteractions(repository)
        verifyNoInteractions(preferencesService)
        verifyNoInteractions(userEventProfileService)
        verifyNoInteractions(transactionalOperator)
        verifyNoInteractions(roleService)
    }

    @Test
    fun `Should assignableUserRoles not throw`() {
        // Arrange
        whenever(roleService.getAssignableUserRoles(any())).thenReturn(emptyList())

        // Act
        service.assignableUserRoles(currentUser()).blockFirst()

        // Assert
        verify(roleService).getAssignableUserRoles(currentUser())
    }

    @Test
    fun `Should createUser create and return User`() {
        // Arrange
        val userOidcId = UUID.randomUUID()
        val userFirstName = "John"
        val userLastName = "DOE"
        val userEmail = "$userFirstName.$userLastName@test.com"
        val user = UserModel().apply {
            oidcId = userOidcId
            firstName = userFirstName
            lastName = userLastName
            email = userEmail
        }

        whenever(repository.create(any())).thenReturn(Mono.just(user))
        whenever(preferencesService.findByUser(any())).thenReturn(Mono.just(PreferencesModel(userId = user.id)))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        service.createUser(userOidcId, userEmail, userFirstName, userLastName).block()

        // Assert
        verify(repository).create(any())
        verify(preferencesService).findByUser(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserIfPersonalDataChanged update and return User`(oldUser: UserModel) {
        // Arrange
        val firstName = "John"
        val lastName = "DOE"
        val email = "$firstName.$lastName@test.com"

        whenever(repository.update(any())).thenReturn(Mono.just(UserModel()))

        // Act
        service.updateUserIfPersonalDataChanged(CurrentUserModel(oldUser), email, firstName, lastName).block()

        // Assert
        verify(repository).update(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserRoleById update and return User`(
        currentRoleLevel: Int?,
        roleToAssign: String?,
        expectedVerifyLastLevel0: Int
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val userToUpdateRole = if (Objects.nonNull(currentRoleLevel)) USER_ROLE else null
        val userToUpdate = UserModel().apply { id = uuid; role = userToUpdateRole }
        val currentUser = currentUser().apply { role = USER_ROLE }

        whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        whenever(repository.findById(any(), any())).thenReturn(Mono.just(userToUpdate))
        whenever(repository.findByRoleLevel(any(), any())).thenReturn(Flux.just(UserModel()))
        whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(currentRoleLevel)
        whenever(repository.update(any())).thenReturn(Mono.just(UserModel()))

        // Act
        service.updateUserRoleById(currentUser, uuid, roleToAssign).block()

        // Assert
        verify(roleService).getAssignableUserRoles(currentUser)
        verify(roleService).getLevelByUserRole(userToUpdateRole)
        verify(repository, times(expectedVerifyLastLevel0)).findByRoleLevel(roleLevel = 0, visibilitySearched = true)
        verify(repository).update(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserRoleById throw RegistryException`(
        currentRoleLevel: Int?,
        assignableRoles: List<String>,
        roleToAssign: String?,
        userToUpdate: UserModel,
        level0Users: Array<UserModel>,
        expectedErrorStatus: HttpStatus,
        expectedError: String,
        expectedGetRoleLevel: Int,
        expectedVerifyLastLevel0: Int
    ) {
        // Arrange
        val currentUser = currentUser().apply { role = USER_ROLE }

        whenever(roleService.getAssignableUserRoles(any())).thenReturn(assignableRoles)
        whenever(repository.findById(any(), any())).thenReturn(Mono.just(userToUpdate))
        whenever(repository.findByRoleLevel(any(), any())).thenReturn(Flux.just(*level0Users))
        whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(currentRoleLevel)
        whenever(repository.update(any())).thenReturn(Mono.just(UserModel()))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateUserRoleById(currentUser, userToUpdate.id !!, roleToAssign).block()
        }) as RegistryException

        // Assert
        assertEquals(expectedErrorStatus, result.status)
        assertEquals(expectedError, result.message)
        verify(roleService).getAssignableUserRoles(currentUser)
        verify(roleService, times(expectedGetRoleLevel)).getLevelByUserRole(USER_ROLE)
        verify(repository, times(expectedVerifyLastLevel0)).findByRoleLevel(roleLevel = 0, visibilitySearched = true)
        verify(repository, never()).update(any())
    }

    @Test
    fun `Should blockUserById block and return User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
        val currentUser = currentUser().apply { role = USER_ROLE }

        whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        whenever(repository.findById(any(), any())).thenReturn(Mono.just(foundUser))
        whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
        whenever(repository.update(any())).thenReturn(Mono.just(UserModel()))
        whenever(
            userEventProfileService.validateNotLastEventRoleLevel0(
                any(),
                anyOrNull(),
                any(),
                any()
            )
        ).thenReturn(Mono.just(foundUser))

        // Act
        service.blockUserById(currentUser, uuid).block()

        // Assert
        verify(roleService).getAssignableUserRoles(currentUser)
        verify(roleService).getLevelByUserRole(USER_ROLE)
        verify(repository).update(any())
        verify(userEventProfileService).validateNotLastEventRoleLevel0(
            uuid,
            eventId = null,
            foundUser,
            error = USER_BLOCK_LAST_EVENT_ADMINISTRATOR
        )
    }

    @Test
    fun `Should blockUserById throw when user is current user`() {
        // Arrange
        val uuid = currentUser().id !!
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }

        whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        whenever(repository.findById(any(), any())).thenReturn(Mono.just(foundUser))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.blockUserById(currentUser(), uuid).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(USER_BLOCK_CURRENT_USER, result.message)
        verify(repository).findById(uuid, visibilitySearched = true)
        verify(roleService).getAssignableUserRoles(currentUser())
        verify(roleService, never()).getLevelByUserRole(anyOrNull())
        verify(userEventProfileService, never()).validateNotLastEventRoleLevel0(any(), anyOrNull(), any(), any())
        verify(repository, never()).update(any())
    }

    @Test
    fun `Should unblockUserById block and return User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
        val currentUser = currentUser().apply { role = USER_ROLE }

        whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        whenever(repository.findById(any(), any())).thenReturn(Mono.just(foundUser))
        whenever(repository.update(any())).thenReturn(Mono.just(UserModel()))

        // Act
        service.unblockUserById(currentUser, uuid).block()

        // Assert
        verify(roleService).getAssignableUserRoles(currentUser)
        verify(repository).update(any())
    }

    @Test
    fun `Should impersonateUserById block and return User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
        val currentUser = currentUser().apply { role = USER_ROLE }

        whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(foundUser))
        whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
        whenever(repository.update(any())).thenReturn(Mono.just(UserModel()))
        whenever(
            userEventProfileService.validateNotLastEventRoleLevel0(
                any(),
                anyOrNull(),
                any(),
                any()
            )
        ).thenReturn(Mono.just(foundUser))

        // Act
        service.impersonateUserById(currentUser, uuid).block()

        // Assert
        verify(roleService).getAssignableUserRoles(currentUser)
        verify(roleService).getLevelByUserRole(USER_ROLE)
        verify(repository).update(any())
        verify(userEventProfileService).validateNotLastEventRoleLevel0(
            uuid,
            eventId = null,
            foundUser,
            USER_IMPERSONATE_LAST_EVENT_ADMINISTRATOR
        )
    }

    @Test
    fun `Should deleteUserById block and return User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
        val currentUser = currentUser().apply { role = USER_ROLE }

        whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(foundUser))
        whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())
        whenever(
            userEventProfileService.validateNotLastEventRoleLevel0(
                any(),
                anyOrNull(),
                any(),
                any()
            )
        ).thenReturn(Mono.just(foundUser))

        // Act
        service.deleteUserById(currentUser, uuid).block()

        // Assert
        verify(roleService).getAssignableUserRoles(currentUser)
        verify(roleService).getLevelByUserRole(USER_ROLE)
        verify(repository).deleteById(any())
        verify(userEventProfileService).validateNotLastEventRoleLevel0(
            uuid,
            eventId = null,
            foundUser,
            USER_DELETE_LAST_EVENT_ADMINISTRATOR
        )
    }
}
