package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

class SecurityV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var mapper: CurrentUserReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/authentication"
	}

	@Test
	fun `Should findCurrentUser return 200`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/user/current", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<CurrentUserModel>(OK)
		verify(mapper).toDto(any())
	}

	@Test
	fun `Should findCurrentUser return 401 without authentication`() {
		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/user/current", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.expectStatus().isEqualTo(UNAUTHORIZED)
	}

	@Test
	fun `Should the v2 OAuth2 broker endpoints no longer exist`() {
		// Act + Assert
		listOf("/login/uri", "/logout/uri").forEach {
			webClient.authenticate().get().uri("$BASE_URL$it").exchange().expectStatus().isNotFound
		}
		listOf("/token", "/token/refresh").forEach {
			webClient.authenticate().post().uri("$BASE_URL$it").exchange().expectStatus().isNotFound
		}
	}
}
