package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

class PreferencesControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IPreferencesService

    companion object {
        private const val BASE_URL = "/api/users/preferences"
    }

    @Test
    fun `Should updateSelectedEventProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        whenever(service.updateUserPreferenceSelectedEventProfileById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/profile/{profileId}/select", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<PreferencesModel>(OK)
        verify(service).updateUserPreferenceSelectedEventProfileById(any(), eq(uuid))
    }
}
