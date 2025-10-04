package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_BLANK
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
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

class SecurityControllerTest: TestContext() {
	@MockitoBean
	private lateinit var authenticationPort: IAuthenticationPort

	@MockitoBean
	private lateinit var mapper: CurrentUserReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v1/authentication"

		@JvmStatic
		fun `blank redirectUri`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null),
				Arguments.of(""),
			)
		}

		@JvmStatic
		fun `Should fetchToken return 400`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of("redirectUri", null, AUTHORIZATION_CODE_BLANK),
				Arguments.of("redirectUri", "", AUTHORIZATION_CODE_BLANK),
				Arguments.of(null, "code", REDIRECT_URI_BLANK),
				Arguments.of("", "code", REDIRECT_URI_BLANK),
			)
		}

		@JvmStatic
		fun `Should refreshToken return 400`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null),
				Arguments.of(""),
			)
		}
	}

	@Test
	fun `Should getLoginUri return 200`() {
		// Arrange
		val redirectUri = "redirectUri"
		whenever(authenticationPort.getLoginUri(any())).thenReturn(AuthenticationUriModel("uri"))

		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/login/uri", emptyList(), listOf("redirectUri" to redirectUri)))
			.exchange()

		// Assert
		result.body<AuthenticationUriModel>(OK)
		verify(authenticationPort).getLoginUri(redirectUri)
	}

	@ParameterizedTest
	@MethodSource("blank redirectUri")
	fun `Should getLoginUri return 400`(redirectUri: String?) {
		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/login/uri", emptyList(), listOf("redirectUri" to redirectUri)))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, REDIRECT_URI_BLANK)
		verifyNoInteractions(authenticationPort)
	}

	@Test
	fun `Should getLogoutUri return 200`() {
		// Arrange
		val redirectUri = "redirectUri"
		whenever(authenticationPort.getLogoutUri(any())).thenReturn(AuthenticationUriModel("uri"))

		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/logout/uri", emptyList(), listOf("redirectUri" to redirectUri)))
			.exchange()

		// Assert
		result.body<AuthenticationUriModel>(OK)
		verify(authenticationPort).getLogoutUri(redirectUri)
	}

	@ParameterizedTest
	@MethodSource("blank redirectUri")
	fun `Should getLogoutUri return 400`(redirectUri: String?) {
		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/logout/uri", emptyList(), listOf("redirectUri" to redirectUri)))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, REDIRECT_URI_BLANK)
		verifyNoInteractions(authenticationPort)
	}

	@Test
	fun `Should fetchToken return 200`() {
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

	@ParameterizedTest
	@MethodSource
	fun `Should fetchToken return 400`(
		redirectUri: String?,
		authorizationCode: String?,
		expectedCode: String,
	) {
		// Arrange
		val body = AuthenticationInfoModel(redirectUri, authorizationCode)

		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token", emptyList(), emptyList()))
			.bodyValue(body)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)
		verifyNoInteractions(authenticationPort)
	}

	@Test
	fun `Should refreshToken return 200`() {
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

	@ParameterizedTest
	@MethodSource
	fun `Should refreshToken return 400`(refreshToken: String?) {
		// Arrange
		val body = RefreshAuthenticationInfoModel(refreshToken)

		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.bodyValue(body)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, REFRESH_TOKEN_BLANK)
		verifyNoInteractions(authenticationPort)
	}

	@Test
	fun `Should findCurrentUser return 200`() {
		// Arrange
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
