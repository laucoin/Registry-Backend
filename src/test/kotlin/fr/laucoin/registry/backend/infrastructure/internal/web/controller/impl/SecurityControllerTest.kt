package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.WRONG_AUTHENTICATION_MOD
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.CurrentUserDtoMapper
import fr.laucoin.registry.backend.test.TestContainerDatabase
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [TestContainerDatabase::class])
class SecurityControllerTest(
    @Autowired private val webClient: WebTestClient,
) {
    @MockitoSpyBean
    private lateinit var mapper: CurrentUserDtoMapper

    companion object {
        private const val BASE_URL = "/auth"
    }

    @Test
    fun `Should findToken return 422`() {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .get()
            .uri(uriBuilder("$BASE_URL/token", emptyList(), emptyList()))
            .exchange()

        // Assert
        result.assertError(UNPROCESSABLE_ENTITY, WRONG_AUTHENTICATION_MOD, emptyMap())
        verifyNoInteractions(mapper)
    }

    @Test
    fun `Should findCurrentUser return 200`() {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .get()
            .uri(uriBuilder("$BASE_URL/profile", emptyList(), emptyList()))
            .exchange()

        // Assert
        result.body<CurrentUserModel>(OK)
        verify(mapper, times(1)).toDto(any())
    }
}
