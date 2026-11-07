package fr.laucoin.registry.backend.domain.service.usecase.impl

import org.junit.jupiter.api.Assertions.assertSame
import java.util.UUID
import reactor.core.publisher.Flux
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
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

	@Test
	fun `Should findUsersPage delegate to the service`() {
		// Arrange
		val pageable: PageableModel = mock()
		val searchParams: UserSearchParamModel = mock()
		val expected: Mono<PageModel<UserModel>> = Mono.empty()
		whenever(userService.findUsersPage(pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findUsersPage(pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(userService).findUsersPage(pageable, searchParams)
	}

	@Test
	fun `Should findUserById delegate to the service`() {
		// Arrange
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<UserModel> = Mono.empty()
		whenever(userService.findUserById(id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findUserById(id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(userService).findUserById(id, isVisible)
	}

	@Test
	fun `Should findUserByOidcId delegate to the service`() {
		// Arrange
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<CurrentUserModel> = Mono.empty()
		whenever(userService.findUserByOidcId(id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findUserByOidcId(id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(userService).findUserByOidcId(id, isVisible)
	}

	@Test
	fun `Should findUserByEmail delegate to the service`() {
		// Arrange
		val email: String = "value"
		val isVisible: Boolean? = null
		val expected: Flux<CurrentUserModel> = Flux.empty()
		whenever(userService.findUserByEmail(email, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findUserByEmail(email, isVisible)

		// Assert
		assertSame(expected, result)
		verify(userService).findUserByEmail(email, isVisible)
	}

	@Test
	fun `Should assignableUserRoles delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val expected: Flux<String> = Flux.empty()
		whenever(userService.assignableUserRoles(currentUser)).thenReturn(expected)

		// Act
		val result = useCase.assignableUserRoles(currentUser)

		// Assert
		assertSame(expected, result)
		verify(userService).assignableUserRoles(currentUser)
	}

	@Test
	fun `Should updateUserIfPersonalDataChanged delegate to the service`() {
		// Arrange
		val user: CurrentUserModel = mock()
		val email: String = "value"
		val firstName: String? = null
		val lastName: String? = null
		val expected: Mono<CurrentUserModel> = Mono.empty()
		whenever(userService.updateUserIfPersonalDataChanged(user, email, firstName, lastName)).thenReturn(expected)

		// Act
		val result = useCase.updateUserIfPersonalDataChanged(user, email, firstName, lastName)

		// Assert
		assertSame(expected, result)
		verify(userService).updateUserIfPersonalDataChanged(user, email, firstName, lastName)
	}

	@Test
	fun `Should linkUserToOidcId delegate to the service`() {
		// Arrange
		val user: CurrentUserModel = mock()
		val oidcId: UUID = UUID.randomUUID()
		val expected: Mono<CurrentUserModel> = Mono.empty()
		whenever(userService.linkUserToOidcId(user, oidcId)).thenReturn(expected)

		// Act
		val result = useCase.linkUserToOidcId(user, oidcId)

		// Assert
		assertSame(expected, result)
		verify(userService).linkUserToOidcId(user, oidcId)
	}

	@Test
	fun `Should recordLoginByOidcId delegate to the service`() {
		// Arrange
		val oidcId: UUID = UUID.randomUUID()
		val expected: Mono<Void> = Mono.empty()
		whenever(userService.recordLoginByOidcId(oidcId)).thenReturn(expected)

		// Act
		val result = useCase.recordLoginByOidcId(oidcId)

		// Assert
		assertSame(expected, result)
		verify(userService).recordLoginByOidcId(oidcId)
	}

	@Test
	fun `Should updateUserRoleById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val role: String? = null
		val expected: Mono<UserModel> = Mono.empty()
		whenever(userService.updateUserRoleById(currentUser, id, role)).thenReturn(expected)

		// Act
		val result = useCase.updateUserRoleById(currentUser, id, role)

		// Assert
		assertSame(expected, result)
		verify(userService).updateUserRoleById(currentUser, id, role)
	}

	@Test
	fun `Should blockUserById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<UserModel> = Mono.empty()
		whenever(userService.blockUserById(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.blockUserById(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(userService).blockUserById(currentUser, id)
	}

	@Test
	fun `Should unblockUserById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<UserModel> = Mono.empty()
		whenever(userService.unblockUserById(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.unblockUserById(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(userService).unblockUserById(currentUser, id)
	}

	@Test
	fun `Should deleteUserById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(userService.deleteUserById(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteUserById(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(userService).deleteUserById(currentUser, id)
	}

	@Test
	fun `Should deleteCurrentUser delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val expected: Mono<Unit> = Mono.empty()
		whenever(userService.deleteCurrentUser(currentUser)).thenReturn(expected)

		// Act
		val result = useCase.deleteCurrentUser(currentUser)

		// Assert
		assertSame(expected, result)
		verify(userService).deleteCurrentUser(currentUser)
	}
}
