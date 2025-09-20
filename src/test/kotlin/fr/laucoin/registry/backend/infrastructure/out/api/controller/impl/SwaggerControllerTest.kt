package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.test.web.reactive.server.WebTestClient

class SwaggerControllerTest: TestContext() {

	@Autowired
	private lateinit var webClient: WebTestClient

	@Test
	fun `Should redirect to swagger UI`() {
		// Arrange
		// Act
		val result = webClient.get()
			.uri("/")
			.exchange()

		// Assert
		result
			.expectStatus().isFound
			.expectHeader().valueEquals(LOCATION, "/swagger-ui.html")
	}
}
