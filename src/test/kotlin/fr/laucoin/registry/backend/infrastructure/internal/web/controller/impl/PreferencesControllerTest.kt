package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.test.TestContainerDatabase
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [TestContainerDatabase::class])
class PreferencesControllerTest(
    @Autowired private val webClient: WebTestClient,
) {
    @MockitoBean
    private lateinit var service: IPreferencesService

    companion object {
        private const val BASE_URL = "/api/users/preferences"

        @JvmStatic
        fun `Preferences management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            return Stream.of(
                Arguments.of(PATCH, "$BASE_URL/profile/{profileId}/select", listOf(uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("Preferences management routes")
    fun `Should return 401`(
        method: HttpMethod, uri: String, params: List<String>, body: Any?
    ) {
        // Arrange
        val request = webClient
            .method(method)
            .uri(uriBuilder(uri, params, listOf()))

        if (Objects.nonNull(body)) {
            request.bodyValue(body !!)
        }

        // Act
        val result = request.exchange()

        // Assert
        result.expectStatus().isUnauthorized
        verifyNoInteractions(service)
    }

    @Test
    fun `Should updateSelectedEventProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.updateUserPreferenceSelectedEventProfileById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/profile/{profileId}/select", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ParticipantModel>(OK)
        verify(service, times(1)).updateUserPreferenceSelectedEventProfileById(any(), eq(uuid))
    }
}
