package fr.laucoin.registry.backend.domain.service.usecase.impl

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
}
