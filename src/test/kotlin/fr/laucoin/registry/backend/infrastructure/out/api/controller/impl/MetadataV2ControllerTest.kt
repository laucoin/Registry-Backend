package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.FeaturesReaderDto
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.test.web.reactive.server.WebTestClient

class MetadataV2ControllerTest : TestContext() {
	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/metadata"
	}

	@Test
	fun `Should getFeatures return 200 with the light user switch`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/features", emptyList(), emptyList()))
			.exchange()

		// Assert
		val features = result.body<FeaturesReaderDto>(OK)
		assertEquals(true, features?.lightUser)
	}

	@Test
	fun `Should the enum label endpoints no longer exist`() {
		// Act + Assert
		listOf(
			"/presences/status",
			"/availabilities/status",
			"/profiles/status",
			"/movements/types",
			"/participants/types",
			"/alerts/status",
		).forEach {
			webClient.authenticate()
				.get()
				.uri("$BASE_URL$it")
				.exchange()
				.expectStatus().isEqualTo(NOT_FOUND)
		}
	}
}
