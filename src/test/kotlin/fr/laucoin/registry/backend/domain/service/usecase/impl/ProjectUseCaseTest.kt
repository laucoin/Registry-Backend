package fr.laucoin.registry.backend.domain.service.usecase.impl

import org.junit.jupiter.api.Assertions.assertSame
import reactor.core.publisher.Flux
import java.util.UUID
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.service.core.IProjectService
import fr.laucoin.registry.backend.domain.service.core.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.support.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectUseCase
import fr.laucoin.registry.backend.test.ModelExt.defaultProject
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectProfile
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class ProjectUseCaseTest {
	private val projectService: IProjectService = mock()
	private val profileService: IUserProjectProfileService = mock()
	private val principalCache: IPrincipalCacheService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val useCase: IProjectUseCase = ProjectUseCase(projectService, profileService, principalCache, transactionalOperator)

	@Test
	fun `Should createProject create the project then its administrator profile and return the project with profile`() {
		// Arrange
		val withProfile = defaultProject().apply { name = "with profile" }
		whenever(projectService.createProject(any(), any())).thenReturn(Mono.just(defaultProject()))
		whenever(profileService.createUserProjectProfileFromProject(any(), any()))
			.thenReturn(Mono.just(defaultProjectProfile()))
		whenever(projectService.findProjectWithProfileById(any(), any())).thenReturn(Mono.just(withProfile))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		val result = useCase.createProject(currentUser(), defaultProject()).block()

		// Assert
		assertEquals(withProfile, result)
		inOrder(projectService, profileService) {
			verify(projectService).createProject(currentUser(), defaultProject())
			verify(profileService).createUserProjectProfileFromProject(currentUser(), defaultProject())
			verify(projectService).findProjectWithProfileById(currentUser(), defaultProjectId)
		}
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createProject invalidate the cached principal of the creator so the new profile grants its authorities`() {
		// Arrange
		whenever(projectService.createProject(any(), any())).thenReturn(Mono.just(defaultProject()))
		whenever(profileService.createUserProjectProfileFromProject(any(), any()))
			.thenReturn(Mono.just(defaultProjectProfile()))
		whenever(projectService.findProjectWithProfileById(any(), any())).thenReturn(Mono.just(defaultProject()))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		useCase.createProject(currentUser(), defaultProject()).block()

		// Assert
		verify(principalCache).invalidate(currentUser().oidcId)
	}

	@Test
	fun `Should createProject propagate the error and keep the cache when the profile creation fails`() {
		// Arrange
		whenever(projectService.createProject(any(), any())).thenReturn(Mono.just(defaultProject()))
		whenever(profileService.createUserProjectProfileFromProject(any(), any()))
			.thenReturn(Mono.error(IllegalStateException("boom")))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			useCase.createProject(currentUser(), defaultProject()).block()
		})

		// Assert
		assertEquals("boom", result.message)
		verify(principalCache, never()).invalidate(any())
		verify(projectService, never()).findProjectWithProfileById(any(), any())
	}

	@Test
	fun `Should findProjectsPage delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val pageable: PageableModel = mock()
		val withProfile: Boolean? = null
		val searchParams: ProjectSearchParamModel = mock()
		val expected: Mono<PageModel<ProjectModel>> = Mono.empty()
		whenever(projectService.findProjectsPage(currentUser, pageable, withProfile, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findProjectsPage(currentUser, pageable, withProfile, searchParams)

		// Assert
		assertSame(expected, result)
		verify(projectService).findProjectsPage(currentUser, pageable, withProfile, searchParams)
	}

	@Test
	fun `Should findProjectById delegate to the service`() {
		// Arrange
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<ProjectModel> = Mono.empty()
		whenever(projectService.findProjectById(id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findProjectById(id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(projectService).findProjectById(id, isVisible)
	}

	@Test
	fun `Should findProjectWithProfileById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ProjectModel> = Mono.empty()
		whenever(projectService.findProjectWithProfileById(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.findProjectWithProfileById(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(projectService).findProjectWithProfileById(currentUser, id)
	}

	@Test
	fun `Should availableProjectOptions delegate to the service`() {
		// Arrange
		val expected: Flux<ProjectOptionEnum> = Flux.empty()
		whenever(projectService.availableProjectOptions()).thenReturn(expected)

		// Act
		val result = useCase.availableProjectOptions()

		// Assert
		assertSame(expected, result)
		verify(projectService).availableProjectOptions()
	}

	@Test
	fun `Should updateProjectById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val project: ProjectModel = mock()
		val expected: Mono<ProjectModel> = Mono.empty()
		whenever(projectService.updateProjectById(currentUser, id, project)).thenReturn(expected)

		// Act
		val result = useCase.updateProjectById(currentUser, id, project)

		// Assert
		assertSame(expected, result)
		verify(projectService).updateProjectById(currentUser, id, project)
	}

	@Test
	fun `Should disableProjectById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ProjectModel> = Mono.empty()
		whenever(projectService.disableProjectById(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.disableProjectById(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(projectService).disableProjectById(currentUser, id)
	}

	@Test
	fun `Should enableProjectById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ProjectModel> = Mono.empty()
		whenever(projectService.enableProjectById(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.enableProjectById(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(projectService).enableProjectById(currentUser, id)
	}

	@Test
	fun `Should addFavoriteProject delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(projectService.addFavoriteProject(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.addFavoriteProject(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(projectService).addFavoriteProject(currentUser, id)
	}

	@Test
	fun `Should removeFavoriteProject delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(projectService.removeFavoriteProject(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.removeFavoriteProject(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(projectService).removeFavoriteProject(currentUser, id)
	}

	@Test
	fun `Should deleteProjectById delegate to the service`() {
		// Arrange
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(projectService.deleteProjectById(id)).thenReturn(expected)

		// Act
		val result = useCase.deleteProjectById(id)

		// Assert
		assertSame(expected, result)
		verify(projectService).deleteProjectById(id)
	}
}
