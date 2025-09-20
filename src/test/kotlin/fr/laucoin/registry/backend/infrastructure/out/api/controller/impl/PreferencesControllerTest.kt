package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

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

class PreferencesControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IPreferencesService

	@Autowired
	private lateinit var webClient: WebTestClient

	companion object {
		private const val BASE_URL = "/api/users/preferences"
	}

	@Test
	fun `Should updateSelectedProjectProfile return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateUserPreferenceSelectedProjectProfileById(any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/profile/select", emptyList(), listOf(Pair("profileId", uuid))))
			.exchange()

		// Assert
		result.body<PreferencesModel>(OK)
		verify(service).updateUserPreferenceSelectedProjectProfileById(any(), eq(uuid))
	}

	@Test
	fun `Should updateSelectedProjectProfileWithProjectId return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateUserPreferenceSelectedProjectProfileByProjectId(any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/projects/{projectId}/profile/select", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<PreferencesModel>(OK)
		verify(service).updateUserPreferenceSelectedProjectProfileByProjectId(any(), eq(uuid))
	}
}
