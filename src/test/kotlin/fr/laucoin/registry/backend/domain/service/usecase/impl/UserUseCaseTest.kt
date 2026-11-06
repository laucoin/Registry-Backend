package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.core.IPreferencesService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.usecase.IUserUseCase
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class UserUseCaseTest {
	private val userService: IUserService = mock()
	private val preferencesService: IPreferencesService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val useCase: IUserUseCase = UserUseCase(userService, preferencesService, transactionalOperator)

	@Test
	fun `Should createUser attach the preferences to the created user inside a transaction`() {
		// Arrange
		val user = currentUser()
		val preferences = PreferencesModel(userId = user.id)
		whenever(userService.createUser(any(), any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(user))
		whenever(preferencesService.findByUser(any())).thenReturn(Mono.just(preferences))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		val result = useCase.createUser(user.oidcId!!, user.email!!, user.firstName, user.lastName).block()

		// Assert
		assertEquals(preferences, result?.preferences)
		verify(userService).createUser(user.oidcId!!, user.email!!, user.firstName, user.lastName)
		verify(preferencesService).findByUser(user)
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createUser propagate the error and skip the preferences when the user creation fails`() {
		// Arrange
		val user = currentUser()
		whenever(userService.createUser(any(), any(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.error(IllegalStateException("boom")))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			useCase.createUser(user.oidcId!!, user.email!!, user.firstName, user.lastName).block()
		})

		// Assert
		assertEquals("boom", result.message)
		verify(preferencesService, never()).findByUser(any())
	}
}
