package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.test.ModelExt.defaultProject
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectProfile
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectProfileId
import fr.laucoin.registry.backend.test.ModelExt.defaultUser
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream

class UserProjectProfileServiceTest {
	private val port: IProjectProfilePort = mock()
	private val favoritePort: IProjectFavoritePort = mock()
	private val roleService: IRoleService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val principalCache: IPrincipalCacheService = mock()
	private val rowLockPort: IRowLockPort = mock()
	init {
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
	}

	private val service: IUserProjectProfileService = UserProjectProfileService(
		port, favoritePort, roleService, principalCache, rowLockPort, transactionalOperator, Clock.systemUTC()
	)

	init {
		whenever(port.lockLevel0ProjectProfilesByUserId(any())).thenReturn(Flux.empty())
	}

	private companion object {
		private const val PROJECT_ROLE = "PROJECT_ROLE"

		@JvmStatic
		fun `Should validateNotLastProjectRoleLevel0 return the given Object`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null),
				Arguments.of(
					defaultProjectId, ProjectProfileRoleCountModel(defaultProject().apply { id = UUID.randomUUID() }, 0)
				),
				Arguments.of(defaultProjectId, ProjectProfileRoleCountModel(defaultProject(), 2)),
			)
		}

		@JvmStatic
		fun `Should validateNotLastProjectRoleLevel0 throw RegistryException`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(defaultProjectId, ProjectProfileRoleCountModel(defaultProject(), 1)),
				Arguments.of(defaultProjectId, ProjectProfileRoleCountModel(defaultProject(), 1)),
				Arguments.of(null, ProjectProfileRoleCountModel(ProjectModel(), 1)),
			)
		}

	}

	@Test
	fun `Should findProjectProfilesPage call port findProjectProfilesPageByUserId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(status = ACCEPTED)

		whenever(port.findProjectProfilesPageByUserId(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.empty())

		// Act
		service.findProjectProfilesPage(defaultProjectId, pageable, params).block()

		// Assert
		verify(port).findProjectProfilesPageByUserId(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findProjectProfilesPage flag the projects the user marked as favorite`() {
		// Arrange
		val favoriteId = UUID.randomUUID()
		val otherId = UUID.randomUUID()
		val profiles = listOf(
			ProjectProfileModel(project = defaultProject().apply { id = favoriteId }),
			ProjectProfileModel(project = defaultProject().apply { id = otherId }),
			ProjectProfileModel(project = null),
		)
		whenever(port.findProjectProfilesPageByUserId(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(0, 10, 3, 1, profiles)))
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.just(favoriteId))

		// Act
		val result = service.findProjectProfilesPage(defaultUserId, PageableModel(0, 10), ProjectProfileSearchParamModel(status = ACCEPTED)).block()!!

		// Assert
		assertEquals(listOf(true, false), result.content.mapNotNull { it.project?.favorite })
		verify(favoritePort).findFavoriteProjectIds(defaultUserId, listOf(favoriteId, otherId))
	}

	@Test
	fun `Should findProjectProfilesById flag the project as favorite`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val profile = ProjectProfileModel(project = defaultProject().apply { id = projectId })
		val user = currentUser()
		whenever(port.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.just(projectId))

		// Act
		val result = service.findProjectProfilesById(user, UUID.randomUUID()).block()!!

		// Assert
		assertEquals(true, result.project?.favorite)
	}

	@Test
	fun `Should findProjectProfilesById leave a profile without project untouched`() {
		// Arrange
		whenever(port.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(ProjectProfileModel()))

		// Act
		val result = service.findProjectProfilesById(currentUser(), UUID.randomUUID()).block()!!

		// Assert
		assertNull(result.project)
		verify(favoritePort, never()).findFavoriteProjectIds(any(), any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateNotLastProjectRoleLevel0 return the given Object`(
		projectId: UUID?,
		profileCount: ProjectProfileRoleCountModel?
	) {
		// Arrange
		val user = defaultUser()

		whenever(port.findLevel0ProjectProfileRoleByUserId(any(), any()))
			.thenReturn(if (Objects.nonNull(profileCount)) Flux.just(profileCount!!) else Flux.empty())

		// Act
		val result = service
			.validateNotLastProjectRoleLevel0(defaultUserId, projectId = projectId, user, error = "ERROR_MESSAGE")
			.block()

		// Assert
		assertEquals(user, result)

		verify(port).lockLevel0ProjectProfilesByUserId(defaultUserId)
		verify(port).findLevel0ProjectProfileRoleByUserId(defaultUserId, isVisible = true)
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
			service.validateNotLastProjectRoleLevel0(defaultUserId, projectId = projectId, UserModel(), errorMessage)
				.block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(errorMessage, result.message)

		verify(port).findLevel0ProjectProfileRoleByUserId(defaultUserId, isVisible = true)
	}

	@Test
	fun `Should createSupportProjectProfile call port findUserIdsWithProjectProfile and create`() {
		// Arrange
		val profile = defaultProjectProfile().apply { role = PROJECT_ROLE }

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
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = service.createSupportProjectProfile(currentUser(), defaultProjectId).block()

		// Assert
		assertEquals(profile, result)

		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			eq(defaultProjectId),
			eq(listOf(currentUser().id!!)),
			eq(null),
			eq(listOf(ACCEPTED, INVITED)),
			any(),
			any(),
		)

		verify(port).create(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
		verify(principalCache).invalidate(currentUser().oidcId)
	}

	@Test
	fun `Should createSupportProjectProfile call port findUserIdsWithProjectProfile and throw because profile duplicated`() {
		// Arrange
		val profile = defaultProjectProfile().apply { role = PROJECT_ROLE }

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
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createSupportProjectProfile(currentUser(), defaultProjectId).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)

		verify(port).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			eq(defaultProjectId),
			eq(listOf(currentUser().id!!)),
			eq(null),
			eq(listOf(ACCEPTED, INVITED)),
			any(),
			any(),
		)

		verify(port, never()).create(any())
	}

	@Test
	fun `Should createUserProjectProfileFromProject create and return a Profile`() {
		// Arrange
		whenever(port.create(any())).thenReturn(Mono.just(defaultProjectProfile()))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		service.createUserProjectProfileFromProject(currentUser(), ProjectModel().apply { id = defaultProjectId })
			.block()

		// Assert
		verify(port).create(argThat { this.projectId == projectId })
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should updateUserProjectProfileStatusById update Project Profile status is INVITED`() {
		// Arrange
		val profile = defaultProjectProfile().apply { status = INVITED }

		whenever(port.findProjectProfileByUserIdAndId(any(), any(), any())).thenReturn(Mono.just(profile))
		whenever(port.update(any())).thenReturn(Mono.just(profile))

		// Act
		service.updateUserProjectProfileStatusById(currentUser(), defaultProjectProfileId, ACCEPTED).block()

		// Assert
		verify(port).update(any())
		verify(port).findProjectProfileByUserIdAndId(currentUser().id!!, defaultProjectProfileId, isVisible = true)
		verify(principalCache).invalidate(currentUser().oidcId)
	}

	@Test
	fun `Should updateUserProjectProfileStatusById throw RegistryException`() {
		// Arrange
		val profile = defaultProjectProfile().apply { status = ACCEPTED }

		whenever(port.findProjectProfileByUserIdAndId(any(), any(), any())).thenReturn(Mono.just(profile))
		whenever(port.update(any())).thenReturn(Mono.just(profile))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateUserProjectProfileStatusById(currentUser(), defaultProjectProfileId, ACCEPTED).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(defaultProjectProfileId.toString(), result.args?.first())

		verify(port, never()).update(any())
		verify(port).findProjectProfileByUserIdAndId(currentUser().id!!, defaultProjectProfileId, isVisible = true)
		verify(principalCache, never()).invalidate(anyOrNull())
	}

	@Test
	fun `Should deleteUserProjectProfileById delete Profile`() {
		// Arrange
		val profile = defaultProjectProfile().apply { user = currentUser() }

		whenever(port.findLevel0ProjectProfileRoleByUserId(any(), any())).thenReturn(Flux.empty())
		whenever(port.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		service.deleteUserProjectProfileById(currentUser(), defaultProjectProfileId).block()

		// Assert
		verify(port).findProjectProfileByUserIdAndId(currentUser().id!!, defaultProjectProfileId, isVisible = null)
		verify(port).deleteById(defaultProjectProfileId)
		verify(principalCache).invalidate(currentUser().oidcId)
	}

	@Test
	fun `Should deleteUserProjectProfileById throw RegistryException when the Profile is not found`() {
		// Arrange
		whenever(port.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.empty())
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteUserProjectProfileById(currentUser(), defaultProjectProfileId).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		verify(port, never()).deleteById(any())
		verify(principalCache, never()).invalidate(anyOrNull())
	}
}
