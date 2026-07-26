package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.test.ModelExt.commonProject
import fr.laucoin.registry.backend.test.ModelExt.commonProjectProfile
import fr.laucoin.registry.backend.test.ModelExt.commonUser
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.projectProfileId
import fr.laucoin.registry.backend.test.ModelExt.userId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream

class UserProjectProfileServiceTest {
	private val port: IProjectProfilePort = mock()
	private val roleService: IRoleService = mock()
	private val preferencesPort: IPreferencesPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()

	private val auditService: IAuditService = mock<IAuditService>().also { audit ->
		whenever(audit.audit(any<Mono<Any>>(), any(), any(), anyOrNull()))
			.thenAnswer { it.getArgument<Mono<Any>>(0) }
	}
	private val service: IUserProjectProfileService = UserProjectProfileService(
		port, roleService, preferencesPort, transactionalOperator, auditService
	)

	private companion object {
		private const val PROJECT_ROLE = "PROJECT_ROLE"

		@JvmStatic
		fun `Should validateNotLastProjectRoleLevel0 return the given Object`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null),
				Arguments.of(
					projectId, ProjectProfileRoleCountModel(commonProject().apply { id = UUID.randomUUID() }, 0)
				),
				Arguments.of(projectId, ProjectProfileRoleCountModel(commonProject(), 2)),
			)
		}

		@JvmStatic
		fun `Should validateNotLastProjectRoleLevel0 throw RegistryException`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(projectId, ProjectProfileRoleCountModel(commonProject(), 1)),
				Arguments.of(projectId, ProjectProfileRoleCountModel(commonProject(), 1)),
				Arguments.of(null, ProjectProfileRoleCountModel(ProjectModel(), 1)),
			)
		}

		@JvmStatic
		fun `Should createUserProjectProfileFromProject create and return a Profile`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(PreferencesModel(), 1),
				Arguments.of(PreferencesModel().apply { selectedProfile = ProjectProfileModel() }, 0),
			)
		}
	}

	@Test
	fun `Should findProjectProfilesPage call port findProjectProfilesPageByUserId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(statusSearched = ACCEPTED)

		whenever(port.findProjectProfilesPageByUserId(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findProjectProfilesPage(projectId, pageable, params).block()

		// Assert
		verify(port).findProjectProfilesPageByUserId(projectId, pageable, params)
	}

	@Test
	fun `Should findSentInvitationsPage call port findSentInvitationsPageByCreatorId with the caller and cutoff`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val since = ZonedDateTime.now().minusDays(2)
		whenever(port.findSentInvitationsPageByCreatorId(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findSentInvitationsPage(currentUser(), pageable, since).block()

		// Assert
		verify(port).findSentInvitationsPageByCreatorId(currentUser().id!!, pageable, since)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateNotLastProjectRoleLevel0 return the given Object`(
		projectId: UUID?,
		profileCount: ProjectProfileRoleCountModel?
	) {
		// Arrange
		val user = commonUser()

		whenever(port.findLevel0ProjectProfileRoleByUserId(any(), any()))
			.thenReturn(if (Objects.nonNull(profileCount)) Flux.just(profileCount!!) else Flux.empty())

		// Act
		val result = service
			.validateNotLastProjectRoleLevel0(userId, projectId = projectId, user, error = "ERROR_MESSAGE")
			.block()

		// Assert
		assertEquals(user, result)

		verify(port).findLevel0ProjectProfileRoleByUserId(userId, visibilitySearched = true)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateNotLastProjectRoleLevel0 throw RegistryException`(
		projectId: UUID?,
		profileCount: ProjectProfileRoleCountModel
	) {
		// Arrange
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findLevel0ProjectProfileRoleByUserId(any(), any()))
			.thenReturn(Flux.just(profileCount))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.validateNotLastProjectRoleLevel0(userId, projectId = projectId, UserModel(), errorMessage).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(errorMessage, result.message)

		verify(port).findLevel0ProjectProfileRoleByUserId(userId, visibilitySearched = true)
	}

	@Test
	fun `Should createSupportProjectProfile call port findUserIdsWithProjectProfile and create`() {
		// Arrange
		val profile = commonProjectProfile().apply { role = PROJECT_ROLE }

		whenever(roleService.getLevel0RoleFromProjectRoles()).thenReturn(PROJECT_ROLE)
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

		whenever(port.create(any())).thenReturn(Mono.just(profile))
		whenever(preferencesPort.findByUserId(any(), anyOrNull()))
			.thenReturn(Mono.just(PreferencesModel().apply { selectedProfile = profile }))

		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = service.createSupportProjectProfile(currentUser(), projectId).block()

		// Assert
		assertEquals(profile, result)

		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			eq(projectId),
			eq(listOf(currentUser().id!!)),
			eq(null),
			eq(listOf(ACCEPTED, INVITED)),
			any(),
			any(),
		)

		verify(port).create(any())
		verify(preferencesPort).findByUserId(currentUser().id!!, visibilitySearched = null)
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createSupportProjectProfile call port findUserIdsWithProjectProfile and throw because profile duplicated`() {
		// Arrange
		val profile = commonProjectProfile().apply { role = PROJECT_ROLE }

		whenever(roleService.getLevel0RoleFromProjectRoles()).thenReturn(PROJECT_ROLE)
		whenever(
			port.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
				any(),
				any(),
				anyOrNull(),
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Flux.just(currentUser().id!!))

		whenever(port.create(any())).thenReturn(Mono.just(profile))
		whenever(preferencesPort.findByUserId(any(), anyOrNull()))
			.thenReturn(Mono.just(PreferencesModel().apply { selectedProfile = profile }))

		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createSupportProjectProfile(currentUser(), projectId).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)

		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			eq(projectId),
			eq(listOf(currentUser().id!!)),
			eq(null),
			eq(listOf(ACCEPTED, INVITED)),
			any(),
			any(),
		)

		verify(port, never()).create(any())
		verify(preferencesPort, never()).findByUserId(any(), anyOrNull())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createUserProjectProfileFromProject create and return a Profile`(
		userPreferences: PreferencesModel,
		expectedCallToUpdatePreferences: Int
	) {
		// Arrange
		whenever(port.create(any())).thenReturn(Mono.just(commonProjectProfile()))
		whenever(preferencesPort.findByUserId(any(), anyOrNull())).thenReturn(Mono.just(userPreferences))
		whenever(preferencesPort.save(any())).thenReturn(Mono.just(userPreferences))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		service.createUserProjectProfileFromProject(currentUser(), ProjectModel()).block()

		// Assert
		verify(port).create(any())
		verify(preferencesPort).findByUserId(currentUser().id!!, visibilitySearched = null)
		verify(preferencesPort, times(expectedCallToUpdatePreferences)).save(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should updateUserProjectProfileStatusById update Project Profile status is INVITED`() {
		// Arrange
		val profile = commonProjectProfile().apply { status = INVITED }

		whenever(port.findProjectProfileByUserIdAndId(any(), any(), any())).thenReturn(Mono.just(profile))
		whenever(port.update(any())).thenReturn(Mono.just(profile))

		// Act
		service.updateUserProjectProfileStatusById(currentUser(), projectProfileId, ACCEPTED).block()

		// Assert
		verify(port).update(any())
		verify(port).findProjectProfileByUserIdAndId(currentUser().id!!, projectProfileId, visibilitySearched = true)
	}

	@Test
	fun `Should toggleFavorite flip the favorite flag on the caller's own profile`() {
		// Arrange
		val profile = commonProjectProfile().apply { favorite = false }
		whenever(port.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(port.update(any())).thenAnswer { Mono.just(it.getArgument(0)) }

		// Act
		service.toggleFavorite(currentUser(), projectProfileId).block()

		// Assert
		assertTrue(profile.favorite)
		verify(port).findProjectProfileByUserIdAndId(currentUser().id!!, projectProfileId, visibilitySearched = null)
		verify(port).update(any())
	}

	@Test
	fun `Should toggleFavorite throw NOT_FOUND when the caller has no such profile`() {
		// Arrange
		whenever(port.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.toggleFavorite(currentUser(), projectProfileId).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should updateUserProjectProfileStatusById throw RegistryException`() {
		// Arrange
		val profile = commonProjectProfile().apply { status = ACCEPTED }

		whenever(port.findProjectProfileByUserIdAndId(any(), any(), any())).thenReturn(Mono.just(profile))
		whenever(port.update(any())).thenReturn(Mono.just(profile))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateUserProjectProfileStatusById(currentUser(), projectProfileId, ACCEPTED).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(projectProfileId.toString(), result.args?.first())

		verify(port, never()).update(any())
		verify(port).findProjectProfileByUserIdAndId(currentUser().id!!, projectProfileId, visibilitySearched = true)
	}

	@Test
	fun `Should deleteUserProjectProfileById delete Profile`() {
		// Arrange
		val profile = commonProjectProfile().apply { user = currentUser() }

		whenever(port.findLevel0ProjectProfileRoleByUserId(any(), any())).thenReturn(Flux.empty())
		whenever(port.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteUserProjectProfileById(currentUser(), projectProfileId).block()

		// Assert
		verify(port).findProjectProfileByUserIdAndId(currentUser().id!!, projectProfileId, visibilitySearched = null)
		verify(port).deleteById(projectProfileId)
	}

	@Test
	fun `Should deleteUserProjectProfileById throw NOT_FOUND when the caller has no such profile`() {
		// Arrange
		whenever(port.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteUserProjectProfileById(currentUser(), projectProfileId).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(projectProfileId.toString(), result.args?.first())

		verify(port).findProjectProfileByUserIdAndId(currentUser().id!!, projectProfileId, visibilitySearched = null)
		verify(port, never()).deleteById(any())
	}
}
