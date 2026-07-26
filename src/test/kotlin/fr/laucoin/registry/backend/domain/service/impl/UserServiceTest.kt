package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.USER_DELETE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.test.ModelExt.commonUser
import fr.laucoin.registry.backend.test.ModelExt.userId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.util.ReflectionTestUtils.setField
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream

class UserServiceTest {
	private val port: IUserPort = mock()
	private val preferencesService: IPreferencesService = mock()
	private val userProjectProfileService: IUserProjectProfileService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val roleService: IRoleService = mock()

	private val auditService: IAuditService = mock<IAuditService>().also { audit ->
		whenever(audit.audit(any<Mono<Any>>(), any(), any(), anyOrNull()))
			.thenAnswer { it.getArgument<Mono<Any>>(0) }
	}
	private val service = UserService(
		port, preferencesService, userProjectProfileService, transactionalOperator, roleService, auditService
	)

	private val serviceAccountId = UUID.randomUUID()
	private val serviceAccount = CurrentUserModel().apply { id = serviceAccountId; role = USER_ROLE }

	private companion object {
		@JvmStatic
		fun `Should updateUserIfPersonalDataChanged update and return User`(): Stream<Arguments> = Stream.of(
			Arguments.of(UserModel()),
			Arguments.of(commonUser()),
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

		/**
		 * The last two last-administrator cases pair the user under update with a level-0 user
		 * carrying an equal but NOT identical UUID instance, which is the shape the running
		 * application produces: the two models come from separate queries, so the identity the
		 * other cases share is an artefact of the fixture. Keep them — an identity comparison in
		 * the guard passes every same-instance case and still lets the last administrator be
		 * demoted in production.
		 */
		@JvmStatic
		fun `Should updateUserRoleById throw RegistryException`(): Stream<Arguments> {
			val uuid = UUID.randomUUID()
			val sameUuidDistinctInstance = UUID.fromString(uuid.toString())
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
					CONFLICT,
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
					CONFLICT,
					USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE,
					0,
					1,
				),
				Arguments.of(
					0,
					listOf(USER_ROLE),
					USER_ROLE,
					UserModel().apply { id = uuid; role = USER_ROLE },
					arrayOf(UserModel().apply { id = sameUuidDistinctInstance; role = USER_ROLE }),
					CONFLICT,
					USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE,
					1,
					1,
				),
				Arguments.of(
					0,
					listOf(USER_ROLE),
					USER_ROLE,
					UserModel().apply { id = uuid },
					arrayOf(UserModel().apply { id = sameUuidDistinctInstance }),
					CONFLICT,
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
	fun `Should onApplicationProject call port findServiceAccount`() {
		// Arrange
		val event: ContextRefreshedEvent = mock()
		val serviceAccount = CurrentUserModel()

		whenever(port.findServiceAccount()).thenReturn(Mono.just(serviceAccount))

		// Act
		service.onApplicationEvent(event)

		// Assert
		verify(port).findServiceAccount()
	}

	@Test
	fun `Should findUsersPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = UserSearchParamModel()

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findUsersPage(pageable, params).block()

		// Assert
		verify(port).findPage(pageable, params, emptyList())
	}

	@Test
	fun `Should findUserById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(commonUser()))

		// Act
		service.findUserById(userId, onlyVisible).block()

		// Assert
		verify(port).findById(userId, onlyVisible)
	}

	@Test
	fun `Should findUserById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), anyOrNull()))
			.thenReturn(Mono.just(UserModel().apply { id = serviceAccountId }))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findUserById(serviceAccountId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(serviceAccountId.toString(), result.args?.first())

		verify(port).findById(serviceAccountId, onlyVisible)
	}

	@Test
	fun `Should findUserByOidcId call port findByOidcId`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findByOidcId(any(), anyOrNull())).thenReturn(Mono.just(currentUser()))

		// Act
		service.findUserByOidcId(currentUser().oidcId!!, onlyVisible).block()

		// Assert
		verify(port).findByOidcId(currentUser().oidcId!!, onlyVisible)
	}

	@Test
	fun `Should findUserByEmail call port findByEmail`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findByEmail(any(), anyOrNull())).thenReturn(Flux.just(currentUser()))

		// Act
		service.findUserByEmail(currentUser().email!!, onlyVisible).collectList().block()

		// Assert
		verify(port).findByEmail(currentUser().email!!, onlyVisible)
	}

	@Test
	fun `Should serviceAccount return service account`() {
		// Act
		val result = service.serviceAccount()

		// Assert
		assertEquals(serviceAccount, result)
		verifyNoInteractions(port)
		verifyNoInteractions(preferencesService)
		verifyNoInteractions(userProjectProfileService)
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
		val user = currentUser()

		whenever(port.create(any())).thenReturn(Mono.just(user))
		whenever(preferencesService.findByUser(any())).thenReturn(Mono.just(PreferencesModel(userId = user.id)))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		service.createUser(user.oidcId!!, user.email!!, user.firstName, user.lastName).block()

		// Assert
		verify(port).create(any())
		verify(preferencesService).findByUser(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should linkUser set oidc id, personal data and last login, keep role and persist`() {
		// Arrange
		val invited = CurrentUserModel().apply { id = userId; role = USER_ROLE; oidcId = null }
		val newOidcId = UUID.randomUUID()

		whenever(port.update(any())).thenReturn(Mono.just(UserModel()))

		// Act
		service.linkUser(invited, newOidcId, "jane.doe@test.com", "Jane", "DOE").block()

		// Assert
		assertEquals(newOidcId, invited.oidcId)
		assertEquals("jane.doe@test.com", invited.email)
		assertEquals("Jane", invited.firstName)
		assertEquals("DOE", invited.lastName)
		assertEquals(USER_ROLE, invited.role)
		assertNotNull(invited.lastLogin)
		verify(port).update(any())
	}

	@Test
	fun `Should findOrCreateInvitedUser return existing user when email matches`() {
		// Arrange
		whenever(port.findByEmail(any(), anyOrNull())).thenReturn(Flux.just(currentUser()))

		// Act
		val result = service.findOrCreateInvitedUser(currentUser().email!!, currentUser()).block()

		// Assert
		assertEquals(currentUser().id, result?.id)
		verify(port).findByEmail(currentUser().email!!, visibilitySearched = null)
		verify(port, never()).create(any())
	}

	@Test
	fun `Should findOrCreateInvitedUser create email-only user when none matches`() {
		// Arrange
		val inviter = currentUser().apply { role = USER_ROLE }
		whenever(port.findByEmail(any(), anyOrNull())).thenReturn(Flux.empty())
		whenever(roleService.getDefaultUserRole()).thenReturn(USER_ROLE)
		whenever(port.create(any())).thenAnswer { Mono.just(it.getArgument<UserModel>(0)) }

		// Act
		service.findOrCreateInvitedUser("invited@test.com", inviter).block()

		// Assert
		val captor = argumentCaptor<UserModel>()
		verify(port).create(captor.capture())
		val created = captor.firstValue
		assertNull(created.oidcId)
		assertEquals("invited@test.com", created.email)
		assertNull(created.firstName)
		assertNull(created.lastName)
		assertEquals(USER_ROLE, created.role)
		assertNull(created.lastLogin)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateUserIfPersonalDataChanged update and return User`(oldUser: UserModel) {
		// Arrange
		val firstName = "John"
		val lastName = "DOE"
		val email = "$firstName.$lastName@test.com"

		whenever(port.update(any())).thenReturn(Mono.just(UserModel()))

		// Act
		service.updateUserIfPersonalDataChanged(CurrentUserModel(oldUser), email, firstName, lastName).block()

		// Assert
		verify(port).update(any())
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
		whenever(port.findById(any(), any())).thenReturn(Mono.just(userToUpdate))
		whenever(port.findByRoleLevel(any(), any())).thenReturn(Flux.just(UserModel()))
		whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(currentRoleLevel)
		whenever(port.update(any())).thenReturn(Mono.just(UserModel()))

		// Act
		service.updateUserRoleById(currentUser, uuid, roleToAssign).block()

		// Assert
		verify(roleService).getAssignableUserRoles(currentUser)
		verify(roleService).getLevelByUserRole(userToUpdateRole)
		verify(port, times(expectedVerifyLastLevel0)).findByRoleLevel(roleLevel = 0, visibilitySearched = true)
		verify(port).update(any())
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
		whenever(port.findById(any(), any())).thenReturn(Mono.just(userToUpdate))
		whenever(port.findByRoleLevel(any(), any())).thenReturn(Flux.just(*level0Users))
		whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(currentRoleLevel)
		whenever(port.update(any())).thenReturn(Mono.just(UserModel()))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateUserRoleById(currentUser, userToUpdate.id!!, roleToAssign).block()
		}) as RegistryException

		// Assert
		assertEquals(expectedErrorStatus, result.status)
		assertEquals(expectedError, result.message)

		verify(roleService).getAssignableUserRoles(currentUser)
		verify(roleService, times(expectedGetRoleLevel)).getLevelByUserRole(USER_ROLE)
		verify(port, times(expectedVerifyLastLevel0)).findByRoleLevel(roleLevel = 0, visibilitySearched = true)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should blockUserById block and return User`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val foundUser = commonUser().apply { id = uuid; role = USER_ROLE }
		val currentUser = currentUser().apply { role = USER_ROLE }

		whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
		whenever(port.findById(any(), any())).thenReturn(Mono.just(foundUser))
		whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
		whenever(port.update(any())).thenReturn(Mono.just(UserModel()))
		whenever(
			userProjectProfileService.validateNotLastProjectRoleLevel0(
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
		verify(port).update(any())
		verify(userProjectProfileService).validateNotLastProjectRoleLevel0(
			uuid,
			projectId = null,
			foundUser,
			error = USER_BLOCK_LAST_PROJECT_ADMINISTRATOR
		)
	}

	@Test
	fun `Should blockUserById throw when user is current user`() {
		// Arrange
		val uuid = currentUser().id!!
		val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }

		whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
		whenever(port.findById(any(), any())).thenReturn(Mono.just(foundUser))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.blockUserById(currentUser(), uuid).block()
		}) as RegistryException

		// Assert
		assertEquals(FORBIDDEN, result.status)
		assertEquals(USER_BLOCK_CURRENT_USER, result.message)

		verify(port).findById(uuid, visibilitySearched = true)
		verify(roleService).getAssignableUserRoles(currentUser())
		verify(roleService, never()).getLevelByUserRole(anyOrNull())
		verify(userProjectProfileService, never()).validateNotLastProjectRoleLevel0(any(), anyOrNull(), any(), any())
		verify(port, never()).update(any())
	}

	@Test
	fun `Should unblockUserById block and return User`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
		val currentUser = currentUser().apply { role = USER_ROLE }

		whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
		whenever(port.findById(any(), any())).thenReturn(Mono.just(foundUser))
		whenever(port.update(any())).thenReturn(Mono.just(UserModel()))

		// Act
		service.unblockUserById(currentUser, userId).block()

		// Assert
		verify(roleService).getAssignableUserRoles(currentUser)
		verify(port).update(any())
	}

	/**
	 * Self-service erasure runs the administrative pipeline minus the
	 * not-current-user guard, so the two things worth pinning are that the
	 * last-administrator guards still fire and that the row is really deleted.
	 */
	@Test
	fun `Should deleteCurrentUser delete the caller own account`() {
		// Arrange
		val currentUser = currentUser().apply { role = USER_ROLE }
		val foundUser = UserModel().apply { id = currentUser.id; role = USER_ROLE }

		whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(foundUser))
		whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
		whenever(port.deleteById(any())).thenReturn(Mono.empty())
		whenever(
			userProjectProfileService.validateNotLastProjectRoleLevel0(
				any(),
				anyOrNull(),
				any(),
				any()
			)
		).thenReturn(Mono.just(foundUser))

		// Act
		service.deleteCurrentUser(currentUser).block()

		// Assert
		verify(roleService).getAssignableUserRoles(currentUser)
		verify(roleService).getLevelByUserRole(USER_ROLE)
		verify(port).findById(currentUser.id!!, visibilitySearched = null)
		verify(port).deleteById(currentUser.id!!)
		verify(userProjectProfileService).validateNotLastProjectRoleLevel0(
			currentUser.id!!,
			projectId = null,
			foundUser,
			USER_DELETE_LAST_PROJECT_ADMINISTRATOR
		)
		verify(auditService).audit(any<Mono<Unit>>(), eq(currentUser), eq(USER_DELETE), eq(currentUser.id))
	}

	@Test
	fun `Should deleteCurrentUser throw when current user is the last application administrator`() {
		// Arrange
		val currentUser = currentUser().apply { role = USER_ROLE }
		val foundUser = UserModel().apply { id = currentUser.id; role = USER_ROLE }

		whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(foundUser))
		whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(0)
		whenever(port.findByRoleLevel(any(), any())).thenReturn(Flux.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteCurrentUser(currentUser).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(USER_DELETE_LAST_APPLICATION_ADMINISTRATOR, result.message)

		verify(port).findByRoleLevel(roleLevel = 0, visibilitySearched = true)
		verify(userProjectProfileService, never()).validateNotLastProjectRoleLevel0(any(), anyOrNull(), any(), any())
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should deleteUserById block and return User`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val foundUser = UserModel().apply { id = uuid; role = USER_ROLE }
		val currentUser = currentUser().apply { role = USER_ROLE }

		whenever(roleService.getAssignableUserRoles(any())).thenReturn(listOf(USER_ROLE))
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(foundUser))
		whenever(roleService.getLevelByUserRole(anyOrNull())).thenReturn(1)
		whenever(port.deleteById(any())).thenReturn(Mono.empty())
		whenever(
			userProjectProfileService.validateNotLastProjectRoleLevel0(
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
		verify(port).deleteById(any())
		verify(userProjectProfileService).validateNotLastProjectRoleLevel0(
			uuid,
			projectId = null,
			foundUser,
			USER_DELETE_LAST_PROJECT_ADMINISTRATOR
		)
	}

	@Test
	fun `Should purgeUsersIfNecessary call not logged user since a date, and call port deleteById`() {
		// Arrange
		val date = LocalDate.EPOCH
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()

		whenever(port.findUserIdsOlderThanLastLogin(any())).thenReturn(Flux.just(uuid1, uuid2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeUsersIfNecessary(date, false).collectList().block()

		// Assert
		verify(port).findUserIdsOlderThanLastLogin(date)
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeUsersIfNecessary call not logged user since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val date = LocalDate.EPOCH

		whenever(port.findUserIdsOlderThanLastLogin(any())).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		service.purgeUsersIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findUserIdsOlderThanLastLogin(date)
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should purgeLightUsersIfNecessary find stale light users and call port deleteById`() {
		// Arrange
		val date = LocalDate.EPOCH
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()

		whenever(port.findLightUserIdsOlderThanCreation(any())).thenReturn(Flux.just(uuid1, uuid2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeLightUsersIfNecessary(date, false).collectList().block()

		// Assert
		verify(port).findLightUserIdsOlderThanCreation(date)
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeLightUsersIfNecessary not call port deleteById because of dryRun`() {
		// Arrange
		val date = LocalDate.EPOCH

		whenever(port.findLightUserIdsOlderThanCreation(any())).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		service.purgeLightUsersIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findLightUserIdsOlderThanCreation(date)
		verify(port, never()).deleteById(any())
	}
}
