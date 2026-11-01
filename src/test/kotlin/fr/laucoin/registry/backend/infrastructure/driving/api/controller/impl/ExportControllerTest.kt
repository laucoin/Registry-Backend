package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserExportModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IExportService
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class ExportControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IExportService

	@Autowired
	private lateinit var webClient: WebTestClient

	private val userExport = UserExportModel(ZonedDateTime.now(ZoneOffset.UTC), UserModel(), null, emptyList(), emptyList())
	private val participantExport = ParticipantExportModel(ZonedDateTime.now(ZoneOffset.UTC), "Camp", ParticipantModel(), emptyList())

	@Test
	fun `Should exportCurrentUser return 200 as an attachment`() {
		// Arrange
		whenever(service.exportUser(any(), any())).thenReturn(Mono.just(userExport))

		// Act
		val result = webClient.authenticate().get()
			.uri(uriBuilder("/api/v2/users/current/export", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.expectStatus().isOk
			.expectHeader().exists(CONTENT_DISPOSITION)
			.expectBody().jsonPath("$.subjectType").isEqualTo("USER")
			.jsonPath("$.schemaVersion").isEqualTo("1.0")
	}

	@Test
	fun `Should exportUserById return 200 for a User administrator`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.exportUser(any(), eq(id))).thenReturn(Mono.just(userExport))

		// Act
		val result = webClient.authenticate(REGISTRY_USER_R).get()
			.uri(uriBuilder("/api/v2/users/{id}/export", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.expectStatus().isOk
		verify(service).exportUser(any(), eq(id))
	}

	@Test
	fun `Should exportUserById return 403 without permission`() {
		// Act
		val result = webClient.authenticate().get()
			.uri(uriBuilder("/api/v2/users/{id}/export", listOf(UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.expectStatus().isForbidden
	}

	@Test
	fun `Should exportParticipantById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(
			service.exportParticipant(
				any(),
				eq(defaultProjectId),
				eq(id)
			)
		).thenReturn(Mono.just(participantExport))

		// Act
		val result = webClient.authenticate().get()
			.uri(
				uriBuilder(
					"/api/v2/projects/{projectId}/participants/{id}/export",
					listOf(defaultProjectId, id),
					emptyList()
				)
			)
			.exchange()

		// Assert
		result.expectStatus().isOk
			.expectBody().jsonPath("$.subjectType").isEqualTo("PARTICIPANT")
	}
}
