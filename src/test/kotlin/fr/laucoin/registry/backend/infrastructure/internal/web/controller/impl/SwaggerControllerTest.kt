package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.test.TestContainerDatabase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [TestContainerDatabase::class])
class SwaggerControllerTest(@Autowired private val webClient: WebTestClient) {
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
