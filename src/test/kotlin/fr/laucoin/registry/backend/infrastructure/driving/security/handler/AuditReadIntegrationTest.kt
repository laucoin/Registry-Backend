package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

class AuditReadIntegrationTest : TestContext() {
	@MockitoBean
	private lateinit var auditService: IAuditService

	@MockitoBean
	private lateinit var userService: IUserService

	@Autowired
	private lateinit var webClient: WebTestClient

	private val userId = UUID.randomUUID()

	@Test
	fun `Should the user read be audited as a success through the whole security chain`() {
		// Arrange
		whenever(userService.findUserById(any(), anyOrNull())).thenReturn(Mono.just(UserModel()))

		// Act
		val result = webClient.authenticate(REGISTRY_USER_R).get().uri(uriBuilder("/api/v2/users/{id}", listOf(userId), emptyList())).exchange()

		// Assert
		result.expectStatus().isOk
		verify(auditService).record(argThat { action.name == "READ" && outcome.name == "SUCCESS" && resourceId == userId })
	}

	@Test
	fun `Should the user read be audited as forbidden when the authorization refuses it`() {
		// Arrange

		// Act
		val result = webClient.authenticate().get().uri(uriBuilder("/api/v2/users/{id}", listOf(userId), emptyList())).exchange()

		// Assert
		result.expectStatus().isForbidden
		verify(auditService).record(argThat { action.name == "READ" && outcome.name == "FORBIDDEN" && resourceId == userId })
		verify(userService, never()).findUserById(any(), anyOrNull())
	}
}
