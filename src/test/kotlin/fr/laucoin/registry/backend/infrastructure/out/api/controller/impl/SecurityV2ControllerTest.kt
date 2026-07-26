package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RefreshAuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

class SecurityV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var authenticationPort: IAuthenticationPort

	@MockitoBean
	private lateinit var mapper: CurrentUserReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/authentication"
	}

	@Test
	fun `Should getLoginUri return 200 as a public endpoint`() {
		// Arrange
		val redirectUri = "redirectUri"
		whenever(authenticationPort.getLoginUri(any())).thenReturn(AuthenticationUriModel("uri"))

		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/login/uri", emptyList(), listOf(Pair("redirectUri", redirectUri))))
			.exchange()

		// Assert
		result.body<AuthenticationUriModel>(OK)
		verify(authenticationPort).getLoginUri(redirectUri)
	}

	@Test
	fun `Should getLoginUri return 400 without redirectUri`() {
		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/login/uri", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, REDIRECT_URI_BLANK)
	}

	@Test
	fun `Should getLogoutUri return 200 as a public endpoint`() {
		// Arrange
		val redirectUri = "redirectUri"
		whenever(authenticationPort.getLogoutUri(any())).thenReturn(AuthenticationUriModel("uri"))

		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/logout/uri", emptyList(), listOf(Pair("redirectUri", redirectUri))))
			.exchange()

		// Assert
		result.body<AuthenticationUriModel>(OK)
		verify(authenticationPort).getLogoutUri(redirectUri)
	}

	@Test
	fun `Should fetchToken return 200 as a public endpoint`() {
		// Arrange
		val body = AuthenticationInfoModel(
			redirectUri = "redirectUri",
			authorizationCode = "code",
		)
		whenever(authenticationPort.getAuthenticationToken(any(), any())).thenReturn(
			Mono.just(
				TokenModel(
					accessToken = "accessToken",
					refreshToken = "refreshToken",
					expiresIn = 3600,
					tokenType = "Bearer",
					refreshExpiresIn = 18000,
				)
			)
		)

		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token", emptyList(), emptyList()))
			.bodyValue(body)
			.exchange()

		// Assert
		result.body<TokenModel>(OK)
		verify(authenticationPort).getAuthenticationToken(body.authorizationCode!!, body.redirectUri!!)
	}

	@Test
	fun `Should refreshToken return 200 as a public endpoint`() {
		// Arrange
		val body = RefreshAuthenticationInfoModel(refreshToken = "refreshToken")
		whenever(authenticationPort.refreshAuthenticationToken(any())).thenReturn(
			Mono.just(
				TokenModel(
					accessToken = "accessToken",
					refreshToken = "refreshToken",
					expiresIn = 3600,
					tokenType = "Bearer",
					refreshExpiresIn = 18000,
				)
			)
		)

		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.bodyValue(body)
			.exchange()

		// Assert
		result.body<TokenModel>(OK)
		verify(authenticationPort).refreshAuthenticationToken(body.refreshToken!!)
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
}
