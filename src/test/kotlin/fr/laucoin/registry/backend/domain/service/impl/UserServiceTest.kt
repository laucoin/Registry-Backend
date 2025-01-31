package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_IMPERSONATE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import org.mockito.kotlin.never
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpStatus.FORBIDDEN
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

    companion object {
        private const val OTHER_USER_ROLE = "OTHER_USER_ROLE"
        private const val USER_ROLE = "USER_ROLE"

        private val user0 = UserModel().apply { lastName = "0" }
        private val user1 = UserModel().apply { lastName = "1" }
        private val user2 = UserModel().apply { lastName = "2" }
        private val user3 = UserModel().apply { lastName = "3" }

        private val users = arrayOf(user0, user1, user2, user3)
        private val serviceAccount = UserModel().apply { id = UUID.randomUUID(); role = USER_ROLE }

        @JvmStatic
        fun `Should findUsers return Users`(): Stream<Arguments> = Stream.of(
            Arguments.of(ASC, null, users.toList()),
            Arguments.of(DESC, null, users.toList().reversed()),
            Arguments.of(ASC, "0", listOf(user0)),
            Arguments.of(ASC, "1", listOf(user1)),
            Arguments.of(ASC, "2", listOf(user2)),
            Arguments.of(ASC, "3", listOf(user3)),
            Arguments.of(DESC, "0", listOf(user0)),
            Arguments.of(DESC, "1", listOf(user1)),
            Arguments.of(DESC, "2", listOf(user2)),
            Arguments.of(DESC, "3", listOf(user3)),
            Arguments.of(ASC, "QWERTY", emptyList<UserModel>()),
            Arguments.of(DESC, "QWERTY", emptyList<UserModel>()),
        )

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
            Arguments.of(1, null, 0),
            Arguments.of(0, null, 1),
        )

        @JvmStatic
        fun `Should updateUserRoleById throw RegistryException`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
            return Stream.of(
                Arguments.of(
                    1,
                    OTHER_USER_ROLE,
                    foundUser,
                    emptyArray<UserModel>(),
                    USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN,
                    2,
                    0,
                    0,
                ),
                Arguments.of(
                    0,
                    USER_ROLE,
                    foundUser,
                    arrayOf(foundUser),
                    USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE,
                    2,
                    1,
                    1,
                ),
            )
        }
    }

    @BeforeEach
    fun setUp() {
        setField(service, "serviceAccount", serviceAccount)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findUsers return Users`(
        order: Direction,
        searched: String?,
        expectedList: List<UserModel>,
    ) {
        // Arrange
        setField(service, "searchThreshold", 0.5)
        `when`(repository.findAll(any())).thenReturn(Flux.just(*users, serviceAccount))

        // Act
        val result = service.findUsers(
            order,
            onlyVisible = true,
            searched,
        ).collectList().block()

        // Assert
        assertEquals(expectedList.size, result?.size)
        expectedList.forEachIndexed { index, it ->
            assertEquals(it, result?.get(index))
        }
    }

    @Test
    fun `Should findUserById return the User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(user0))

        // Act
        service.findUserById(uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findById(uuid, onlyVisible = true)
    }

    @Test
    fun `Should findUserByOidcId return the User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val user = CurrentUserModel()
        `when`(repository.findByOidcId(any(), any())).thenReturn(Mono.just(user))

        // Act
        service.findUserByOidcId(uuid, onlyVisible = true).block()

        // Assert
        verify(repository, times(1)).findByOidcId(uuid, onlyVisible = true)
    }

    @Test
    fun `Should getServiceAccount return the service account`() {
        // Arrange
        // Act
        val result = service.getServiceAccount()

        // Assert
        assertEquals(serviceAccount, result)
    }

    @Test
    fun `Should createUser create and return User`() {
        // Arrange
        val oidcId = UUID.randomUUID()
        val firstName = "John"
        val lastName = "DOE"
        val email = "$firstName.$lastName@test.com"

        `when`(repository.create(any())).thenReturn(Mono.just(user0))
        `when`(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(user0))

        // Act
        service.createUser(oidcId, email, firstName, lastName).block()

        // Assert
        verify(repository, times(1)).create(any())
        verify(transactionalOperator, times(1)).transactional(any<Mono<*>>())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserIfPersonalDataChanged update and return User`(oldUser: UserModel) {
        // Arrange
        val firstName = "John"
        val lastName = "DOE"
        val email = "$firstName.$lastName@test.com"

        `when`(repository.update(any())).thenReturn(Mono.just(user0))

        // Act
        service.updateUserIfPersonalDataChanged(CurrentUserModel(oldUser), email, firstName, lastName).block()

        // Assert
        verify(repository, times(1)).update(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserRoleById update and return User`(
        currentRoleLevel: Int,
        roleToAssign: String?,
        expectedVerifyLastLevel0: Int
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
        val currentUserId = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = currentUserId; role = USER_ROLE }

        `when`(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(foundUser))
        lenient().`when`(repository.findByRoleLevel(any(), any())).thenReturn(Flux.just(user0))
        `when`(roleService.getLevelByUserRole(anyOrNull())).thenReturn(currentRoleLevel)
        `when`(repository.update(any())).thenReturn(Mono.just(user0))

        // Act
        service.updateUserRoleById(currentUser, uuid, roleToAssign).block()

        // Assert
        verify(roleService, times(2)).getAssignableUserRoles(currentUser)
        verify(roleService, times(1)).getLevelByUserRole(USER_ROLE)
        verify(repository, times(expectedVerifyLastLevel0)).findByRoleLevel(roleLevel = 0, onlyVisible = true)
        verify(repository, times(1)).update(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserRoleById throw RegistryException`(
        currentRoleLevel: Int,
        roleToAssign: String?,
        userToUpdate: UserModel,
        level0Users: Array<UserModel>,
        expectedError: String,
        expectedAssignableRole: Int,
        expectedGetRoleLevel: Int,
        expectedVerifyLastLevel0: Int
    ) {
        // Arrange
        val currentUserId = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = currentUserId; role = USER_ROLE }

        `when`(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(userToUpdate))
        lenient().`when`(repository.findByRoleLevel(any(), any())).thenReturn(Flux.just(*level0Users))
        `when`(roleService.getLevelByUserRole(anyOrNull())).thenReturn(currentRoleLevel)
        `when`(repository.update(any())).thenReturn(Mono.just(user0))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateUserRoleById(currentUser, userToUpdate.id !!, roleToAssign).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(expectedError, result.message)

        verify(roleService, times(expectedAssignableRole)).getAssignableUserRoles(currentUser)
        verify(roleService, times(expectedGetRoleLevel)).getLevelByUserRole(USER_ROLE)
        verify(repository, times(expectedVerifyLastLevel0)).findByRoleLevel(roleLevel = 0, onlyVisible = true)
        verify(repository, never()).update(any())
    }

    @Test
    fun `Should blockUserById block and return User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
        val currentUserId = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = currentUserId; role = USER_ROLE }

        `when`(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(foundUser))
        `when`(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
        `when`(repository.update(any())).thenReturn(Mono.just(user0))
        `when`(
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
        verify(roleService, times(1)).getAssignableUserRoles(currentUser)
        verify(roleService, times(1)).getLevelByUserRole(USER_ROLE)
        verify(repository, times(1)).update(any())
        verify(userEventProfileService, times(1)).validateNotLastEventRoleLevel0(
            uuid,
            eventId = null,
            foundUser,
            error = USER_BLOCK_LAST_EVENT_ADMINISTRATOR
        )
    }

    @Test
    fun `Should unblockUserById block and return User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
        val currentUserId = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = currentUserId; role = USER_ROLE }

        `when`(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(foundUser))
        `when`(repository.update(any())).thenReturn(Mono.just(user0))

        // Act
        service.unblockUserById(currentUser, uuid).block()

        // Assert
        verify(roleService, times(1)).getAssignableUserRoles(currentUser)
        verify(repository, times(1)).update(any())
    }

    @Test
    fun `Should impersonateUserById block and return User`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
        val currentUserId = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = currentUserId; role = USER_ROLE }

        `when`(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(foundUser))
        `when`(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
        `when`(repository.update(any())).thenReturn(Mono.just(user0))
        `when`(
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
        verify(roleService, times(1)).getAssignableUserRoles(currentUser)
        verify(roleService, times(1)).getLevelByUserRole(USER_ROLE)
        verify(repository, times(1)).update(any())
        verify(userEventProfileService, times(1)).validateNotLastEventRoleLevel0(
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
        val currentUserId = UUID.randomUUID()
        val currentUser = CurrentUserModel().apply { id = currentUserId; role = USER_ROLE }

        `when`(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(foundUser))
        `when`(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
        `when`(repository.deleteById(any())).thenReturn(Mono.empty())
        `when`(
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
        verify(roleService, times(1)).getAssignableUserRoles(currentUser)
        verify(roleService, times(1)).getLevelByUserRole(USER_ROLE)
        verify(repository, times(1)).deleteById(any())
        verify(userEventProfileService, times(1)).validateNotLastEventRoleLevel0(
            uuid,
            eventId = null,
            foundUser,
            USER_DELETE_LAST_EVENT_ADMINISTRATOR
        )
    }
}
