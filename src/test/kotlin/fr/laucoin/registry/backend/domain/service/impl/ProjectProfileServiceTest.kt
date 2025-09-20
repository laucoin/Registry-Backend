package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_BLOCK_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_UPDATE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ProjectProfileServiceTest {
	private val port: IProjectProfilePort = mock()
	private val profileService: IUserProjectProfileService = mock()
	private val roleService: IRoleService = mock()
	private val userPort: IUserPort = mock()
	private val maxUser: Int = 1
	private val service = ProjectProfileService(profileService, port, roleService, userPort, maxUser)

	companion object {
		@JvmStatic
		fun `Should createProjectProfiles call port findUserIdsWithProjectProfile and saveAll`(): Stream<Arguments> {
			val uuid1 = UUID.randomUUID()
			val uuid2 = UUID.randomUUID()
			return Stream.of(
				Arguments.of(listOf(uuid1, uuid2), emptyList<UUID>(), listOf(uuid1, uuid2)),
				Arguments.of(listOf(uuid1, uuid2), listOf(uuid2), listOf(uuid1)),
			)
		}

		@JvmStatic
		fun `Should updateProjectProfileById call port findById, findUserIdsWithProjectProfile and throw because user are not allowed to edit that role`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"INITIAL_ROLE_PROJECT",
					"UPDATED_ROLE_PROJECT",
					"UPDATED_ROLE_PROJECT",
					PROJECT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
				),
				Arguments.of(
					"INITIAL_ROLE_PROJECT",
					"UPDATED_ROLE_PROJECT",
					"INITIAL_ROLE_PROJECT",
					PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
				),
			)
		}
	}

	@Test
	fun `Should findProjectProfilesPage call port findProjectProfilesPageByProjectId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(statusSearched = ACCEPTED)
		whenever(port.findProjectProfilesPageByProjectId(any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findProjectProfilesPage(projectId, pageable, params).block()

		// Assert
		verify(port).findProjectProfilesPageByProjectId(projectId, pageable, params)
	}

	@Test
	fun `Should findProjectProfileById call port findById`() {
		// Arrange
		val profile = ProjectProfileModel().apply {
			role = "PROJECT_ROLE"; project = ProjectModel().apply { id = projectId }
		}
		val uuid = UUID.randomUUID()
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))

		// Act
		service.findProjectProfileById(projectId, uuid, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, uuid, onlyVisible)
	}

	@Test
	fun `Should findProjectProfileById call port findById throw on empty result`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findProjectProfileById(projectId, uuid, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		verify(port).findById(projectId, uuid, onlyVisible)
	}

	@Test
	fun `Should searchUsers call port findWithLimit`() {
		// Arrange
		val text = "text"
		whenever(userPort.findWithLimit(any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchUsers(text).blockFirst()

		// Assert
		verify(userPort).findWithLimit(eq(maxUser), eq(UserSearchParamModel(text, visibilitySearched = true)))
	}

	@Test
	fun `Should getAssignableProjectRoles call port findProjectProfileByProjectAndUserId and role service getAssignableProjectRoles`() {
		// Arrange
		val profile = ProjectProfileModel().apply {
			role = "PROJECT_ROLE"; project = ProjectModel().apply { id = projectId }
		}
		whenever(port.findProjectProfileByProjectAndUserId(any(), any(), anyOrNull())).thenReturn(
			Mono.just(
				profile
			)
		)
		whenever(roleService.getAssignableProjectRoles(any())).thenReturn(emptyList())

		// Act
		service.getAssignableProjectRoles(currentUser(), projectId).blockFirst()

		// Assert
		verify(port).findProjectProfileByProjectAndUserId(
			projectId,
			currentUser().id!!,
			ProjectProfileSearchParamModel(
				visibilitySearched = true,
				availabilitySearched = true,
				statusSearched = listOf(ACCEPTED),
			),
		)
		verify(roleService).getAssignableProjectRoles(profile)
	}

	@Test
	fun `Should getAssignableProjectRoles throw on port findProjectProfileByProjectAndUserId return empty`() {
		// Arrange
		whenever(port.findProjectProfileByProjectAndUserId(any(), any(), anyOrNull())).thenReturn(Mono.empty())
		whenever(roleService.getAssignableProjectRoles(any())).thenReturn(emptyList())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.getAssignableProjectRoles(currentUser(), projectId).blockFirst()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		verify(port).findProjectProfileByProjectAndUserId(
			projectId,
			currentUser().id!!,
			ProjectProfileSearchParamModel(
				visibilitySearched = true,
				availabilitySearched = true,
				statusSearched = listOf(ACCEPTED),
			),
		)
		verify(roleService, never()).getAssignableProjectRoles(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createProjectProfiles call port findUserIdsWithProjectProfile and saveAll`(
		wantedProfileForUserIds: List<UUID>,
		userIdWithExistingProfile: List<UUID>,
		expectedCreatedUserIds: List<UUID>,
	) {
		// Arrange
		val profiles =
			wantedProfileForUserIds.map { ProjectProfileModel().apply { user = UserModel().apply { id = it } } }
		val expectedProfiles = profiles.filter { expectedCreatedUserIds.contains(it.user?.id) }
		whenever(
			port.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
				any(),
				any(),
				anyOrNull(),
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Flux.just(*userIdWithExistingProfile.toTypedArray()))
		whenever(port.saveAll(any())).thenReturn(Flux.just(*expectedProfiles.toTypedArray()))

		// Act
		val result = service.createProjectProfiles(currentUser(), projectId, wantedProfileForUserIds, profiles).block()

		// Assert
		assertEquals(expectedCreatedUserIds, result?.first)
		assertEquals(userIdWithExistingProfile, result?.second)
		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			wantedProfileForUserIds,
			profileIdToExclude = null,
			statusSearched = listOf(ACCEPTED, INVITED),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
		verify(port).saveAll(expectedProfiles)
	}

	@Test
	fun `Should createProjectProfiles call port findUserIdsWithProjectProfile and throw because all profiles duplicated`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val users = listOf(uuid)
		val profile = ProjectProfileModel().apply {
			user = UserModel().apply { id = uuid }
			startAccess = CustomDateTimeModel(LocalDate.EPOCH)
			endAccess = CustomDateTimeModel(LocalDate.EPOCH)
		}
		val profiles = listOf(profile)
		whenever(
			port.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
				any(),
				any(),
				anyOrNull(),
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Flux.just(uuid))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createProjectProfiles(currentUser(), projectId, users, profiles).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)
		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId = eq(projectId),
			userIds = eq(users),
			profileIdToExclude = eq(null),
			statusSearched = eq(listOf(ACCEPTED, INVITED)),
			startDateTimeSearched = any(),
			endDateTimeSearched = any(),
		)
		verify(port, never()).saveAll(any())
	}

	@Test
	fun `Should updateProjectProfileById call port findById, findUserIdsWithProjectProfile, call profileService validateNotLastProjectRoleLevel0, call port findProjectProfileByProjectAndUserId, call roleService getAssignableProjectRoles and finally call port update`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profileRole = "ROLE_PROJECT"
		val currentUserProfile = ProjectProfileModel().apply {
			role = profileRole
			user = UserModel().apply { id = currentUser().id }
			project = ProjectModel().apply { id = projectId }
		}
		val profile = ProjectProfileModel().apply {
			role = profileRole
			user = UserModel().apply { id = uuid }
			project = ProjectModel().apply { id = projectId }
			startAccess = CustomDateTimeModel.MIN
			endAccess = CustomDateTimeModel.MAX
		}
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(
			port.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
				any(),
				any(),
				anyOrNull(),
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Flux.just())
		whenever(profileService.validateNotLastProjectRoleLevel0(any(), any(), any(), any())).thenReturn(
			Mono.just(
				profile
			)
		)
		whenever(port.findProjectProfileByProjectAndUserId(any(), any(), any())).thenReturn(
			Mono.just(
				currentUserProfile
			)
		)
		whenever(roleService.getAssignableProjectRoles(any())).thenReturn(listOf(profileRole))
		whenever(port.update(any())).thenReturn(Mono.just(profile))

		// Act
		val result = service.updateProjectProfileById(currentUser(), projectId, uuid, profile).block()

		// Assert
		assertEquals(profile, result)
		verify(port).findById(projectId, uuid, visibilitySearched = null)
		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId = eq(projectId),
			userIds = eq(listOf(uuid)),
			profileIdToExclude = eq(null),
			statusSearched = eq(listOf(ACCEPTED, INVITED)),
			startDateTimeSearched = any(),
			endDateTimeSearched = any(),
		)
		verify(profileService).validateNotLastProjectRoleLevel0(
			uuid,
			projectId,
			profile,
			PROJECT_PROFILE_UPDATE_LAST_PROJECT_ADMINISTRATOR
		)
		verify(port).findProjectProfileByProjectAndUserId(
			projectId,
			currentUser().id!!,
			ProjectProfileSearchParamModel(
				visibilitySearched = true,
				availabilitySearched = true,
				statusSearched = listOf(ACCEPTED),
			),
		)
		verify(roleService).getAssignableProjectRoles(currentUserProfile)
		verify(port).update(profile)
	}

	@Test
	fun `Should updateProjectProfileById call port findById, findUserIdsWithProjectProfile, call profileService validateNotLastProjectRoleLevel0, call port findProjectProfileByProjectAndUserId, call roleService getAssignableProjectRoles and throw because profile duplicated`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profileRole = "ROLE_PROJECT"
		val profile = ProjectProfileModel().apply {
			role = profileRole
			user = UserModel().apply { id = uuid }
			project = ProjectModel().apply { id = projectId }
		}
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(
			port.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
				any(),
				any(),
				anyOrNull(),
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Flux.just(uuid))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateProjectProfileById(currentUser(), projectId, uuid, profile).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)
		verify(port).findById(projectId, uuid, visibilitySearched = null)
		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			listOf(uuid),
			profileIdToExclude = null,
			statusSearched = listOf(ACCEPTED, INVITED),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
		verify(profileService, never()).validateNotLastProjectRoleLevel0(any(), any(), any(), any())
		verify(port, never()).findProjectProfileByProjectAndUserId(any(), any(), any())
		verify(roleService, never()).getAssignableProjectRoles(any())
		verify(port, never()).update(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateProjectProfileById call port findById, findUserIdsWithProjectProfile and throw because user are not allowed to edit that role`(
		profileToUpdateRole: String,
		profileUpdatedRole: String,
		allowedRoleString: String,
		expectedErrorMessage: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val currentUserProfile = ProjectProfileModel().apply {
			role = "ROLE_PROJECT"
			user = UserModel().apply { id = currentUser().id }
			project = ProjectModel().apply { id = projectId }
		}
		val profileToUpdate = ProjectProfileModel().apply {
			role = profileToUpdateRole
			user = UserModel().apply { id = uuid }
			project = ProjectModel().apply { id = projectId }
		}
		val profileUpdated = ProjectProfileModel().apply {
			role = profileUpdatedRole
			user = UserModel().apply { id = uuid }
			project = ProjectModel().apply { id = projectId }
		}
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profileToUpdate))
		whenever(
			port.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
				any(),
				any(),
				anyOrNull(),
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Flux.empty())
		whenever(port.findProjectProfileByProjectAndUserId(any(), any(), any())).thenReturn(
			Mono.just(
				currentUserProfile
			)
		)
		whenever(roleService.getAssignableProjectRoles(any())).thenReturn(listOf(allowedRoleString))


		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateProjectProfileById(currentUser(), projectId, uuid, profileUpdated).block()
		}) as RegistryException

		// Assert
		assertEquals(FORBIDDEN, result.status)
		assertEquals(expectedErrorMessage, result.message)
		verify(port).findById(projectId, uuid, visibilitySearched = null)
		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			listOf(uuid),
			profileIdToExclude = null,
			statusSearched = listOf(ACCEPTED, INVITED),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
		verify(profileService, never()).validateNotLastProjectRoleLevel0(any(), anyOrNull(), any(), any())
		verify(port).findProjectProfileByProjectAndUserId(
			projectId,
			currentUser().id!!,
			ProjectProfileSearchParamModel(
				visibilitySearched = true,
				availabilitySearched = true,
				statusSearched = listOf(ACCEPTED),
			),
		)
		verify(roleService).getAssignableProjectRoles(currentUserProfile)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should blockProjectProfileById call port findById, call service profile validateNotLastProjectRoleLevel0 and finally call port update`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profile = ProjectProfileModel().apply {
			user = UserModel().apply { id = uuid }
			project = ProjectModel().apply { id = projectId }
			visible = true
		}
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(profileService.validateNotLastProjectRoleLevel0(any(), any(), any(), any())).thenReturn(
			Mono.just(
				profile
			)
		)
		whenever(port.update(any())).thenReturn(Mono.just(profile))

		// Act
		service.blockProjectProfileById(currentUser(), projectId, uuid).block()

		// Assert
		verify(port).findById(projectId, uuid, visibilitySearched = true)
		verify(profileService).validateNotLastProjectRoleLevel0(
			uuid,
			projectId,
			profile,
			PROJECT_PROFILE_BLOCK_LAST_PROJECT_ADMINISTRATOR,
		)
		verify(port).update(profile.apply { visible = false })
	}

	@Test
	fun `Should unblockProjectProfileById call port findById, call service profile validateNotLastProjectRoleLevel0 and finally call port update`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profile = ProjectProfileModel().apply {
			user = UserModel().apply { id = uuid }
			project = ProjectModel().apply { id = projectId }
			visible = false
		}
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(port.update(any())).thenReturn(Mono.just(profile))

		// Act
		service.unblockProjectProfileById(currentUser(), projectId, uuid).block()

		// Assert
		verify(port).findById(projectId, uuid, visibilitySearched = false)
		verify(port).update(profile.apply { visible = true })
	}

	@Test
	fun `Should deleteProjectProfileById call port findById, call service profile validateNotLastProjectRoleLevel0 and finally call port deleteById`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profile = ProjectProfileModel().apply {
			user = UserModel().apply { id = uuid }
			project = ProjectModel().apply { id = projectId }
			visible = true
		}
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(profileService.validateNotLastProjectRoleLevel0(any(), any(), any(), any())).thenReturn(
			Mono.just(
				profile
			)
		)
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteProjectProfileById(currentUser(), projectId, uuid).block()

		// Assert
		verify(port).findById(projectId, uuid, visibilitySearched = null)
		verify(profileService).validateNotLastProjectRoleLevel0(
			uuid,
			projectId,
			profile,
			PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR,
		)
		verify(port).deleteById(uuid)
	}
}
