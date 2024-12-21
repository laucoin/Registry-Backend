package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.test.web.reactive.server.WebTestClient

class SwaggerControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
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
