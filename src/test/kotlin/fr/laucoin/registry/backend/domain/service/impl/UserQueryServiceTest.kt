package fr.laucoin.registry.backend.domain.service.impl

import org.springframework.test.util.ReflectionTestUtils.setField
import org.junit.jupiter.api.BeforeEach
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.test.ModelExt.defaultUser
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.util.UUID

class UserQueryServiceTest {
	private val port: IUserPort = mock()
	private val preferencesService: IPreferencesService = mock()
	private val userProjectProfileService: IUserProjectProfileService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val roleService: IRoleService = mock()
	private val service = UserQueryService(port, roleService, Clock.systemUTC())

	@BeforeEach
	fun setUp() {
		setField(service, "serviceAccount", serviceAccount)
	}



	private val serviceAccountId = UUID.randomUUID()

	private val serviceAccount = CurrentUserModel().apply { id = serviceAccountId; role = USER_ROLE }

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
	fun `Should onApplicationEvent fail when the service account is missing`() {
		// Arrange
		val event: ContextRefreshedEvent = mock()
		whenever(port.findServiceAccount()).thenReturn(Mono.empty())

		// Act
		val action = { service.onApplicationEvent(event) }

		// Assert
		assertThrows(IllegalStateException::class.java, action)
	}

	@Test
	fun `Should findUsersPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = UserSearchParamModel()

		whenever(port.findPage(any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findUsersPage(pageable, params).block()

		// Assert
		verify(port).findPage(pageable, params)
	}

	@Test
	fun `Should findUserById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultUser()))

		// Act
		service.findUserById(defaultUserId, onlyVisible).block()

		// Assert
		verify(port).findById(defaultUserId, onlyVisible)
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
	fun `Should findUsersByEmails normalize the emails and ignore the service account`() {
		// Arrange
		val user = UserModel().apply { id = UUID.randomUUID(); email = "john@doe.fr" }
		whenever(port.findByEmails(any(), anyOrNull())).thenReturn(Flux.just(user, serviceAccount))

		// Act
		val result = service.findUsersByEmails(listOf(" John@Doe.FR ")).collectList().block()

		// Assert
		assertEquals(listOf(user), result)
		verify(port).findByEmails(listOf("john@doe.fr"), null)
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
}
