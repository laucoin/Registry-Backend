package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.domain.model.UserExportModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.support.IAuditService
import fr.laucoin.registry.backend.domain.service.usecase.IExportUseCase
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@TestPropertySource(properties = ["registry.security.rate-limit.export.capacity=2"])
class ExportRateLimitIntegrationTest : TestContext() {
	@MockitoBean
	private lateinit var exportUseCase: IExportUseCase

	@MockitoBean
	private lateinit var auditService: IAuditService

	@Autowired
	private lateinit var webClient: WebTestClient

	@Test
	fun `Should export answer 429 and audit the refusal once the user exceeded the capacity`() {
		// Arrange
		val export = UserExportModel(ZonedDateTime.now(ZoneOffset.UTC), UserModel(), null, emptyList(), emptyList())
		whenever(exportUseCase.exportUser(any(), any(), any())).thenReturn(Mono.just(export))
		val client = webClient.authenticate()
		val uri = uriBuilder("/api/v2/users/current/export", emptyList(), emptyList())

		// Act
		repeat(2) { client.get().uri(uri).exchange().expectStatus().isOk }
		val result = client.get().uri(uri).exchange()

		// Assert
		result.expectStatus().isEqualTo(429)
			.expectHeader().exists(RETRY_AFTER)
		verify(auditService).record(argThat { outcome.name == "RATE_LIMITED" && resourceType.name == "USER" })
	}
}
