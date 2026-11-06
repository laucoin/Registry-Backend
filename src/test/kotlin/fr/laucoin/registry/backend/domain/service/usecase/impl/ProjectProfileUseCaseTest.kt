package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectProfileUseCase
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
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
import java.util.UUID

class ProjectProfileUseCaseTest {
	private val profileService: IProjectProfileService = mock()
	private val userService: IUserService = mock()
	private val rowLockPort: IRowLockPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val useCase: IProjectProfileUseCase =
		ProjectProfileUseCase(profileService, userService, rowLockPort, transactionalOperator)

	private fun invitation(userEmail: String) = ProjectProfileModel().apply { user = UserModel().apply { email = userEmail } }

	@Test
	fun `Should createProjectProfiles lock the project then resolve the users then create the profiles in a transaction`() {
		// Arrange
		val users = mapOf("new@sgdf.fr" to UserModel().apply { id = UUID.randomUUID(); email = "new@sgdf.fr" })
		val profiles = listOf(invitation("new@sgdf.fr"))
		val expected = Pair(listOf("new@sgdf.fr"), emptyList<String>())
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
		whenever(userService.resolveUsersByEmail(any())).thenReturn(Mono.just(users))
		whenever(profileService.createProjectProfiles(any(), any(), any(), any())).thenReturn(Mono.just(expected))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		val result = useCase.createProjectProfiles(currentUser(), defaultProjectId, profiles).block()

		// Assert
		assertEquals(expected, result)
		inOrder(rowLockPort, userService, profileService) {
			verify(rowLockPort).lock(PROJECT, defaultProjectId)
			verify(userService).resolveUsersByEmail(listOf("new@sgdf.fr"))
			verify(profileService).createProjectProfiles(currentUser(), defaultProjectId, profiles, users)
		}
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createProjectProfiles not resolve the users nor create profiles when the lock fails`() {
		// Arrange
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.error(IllegalStateException("lock")))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			useCase.createProjectProfiles(currentUser(), defaultProjectId, listOf(invitation("new@sgdf.fr"))).block()
		})

		// Assert
		assertEquals("lock", result.message)
		verify(userService, never()).resolveUsersByEmail(any())
		verify(profileService, never()).createProjectProfiles(any(), any(), any(), any())
	}

	@Test
	fun `Should createProjectProfiles propagate the error of the users resolution and create no profile`() {
		// Arrange
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
		whenever(userService.resolveUsersByEmail(any())).thenReturn(Mono.error(IllegalStateException("resolve")))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			useCase.createProjectProfiles(currentUser(), defaultProjectId, listOf(invitation("new@sgdf.fr"))).block()
		})

		// Assert
		assertEquals("resolve", result.message)
		verify(profileService, never()).createProjectProfiles(any(), any(), any(), any())
	}
}
