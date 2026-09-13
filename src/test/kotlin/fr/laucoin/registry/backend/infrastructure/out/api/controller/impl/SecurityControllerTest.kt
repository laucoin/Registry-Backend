package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService.Companion.REFRESH_TOKEN_COOKIE
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.Duration
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
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

		private fun token() = TokenModel(
			accessToken = "accessToken",
			refreshToken = "refreshToken",
			expiresIn = 3600,
			tokenType = "Bearer",
		)
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
		whenever(authenticationPort.getLogoutUri(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(AuthenticationUriModel("uri")))

		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/logout/uri", emptyList(), listOf("redirectUri" to redirectUri)))
			.exchange()

		// Assert
		result.body<AuthenticationUriModel>(OK)
		result.expectCookie().maxAge(ACCESS_TOKEN_COOKIE, Duration.ZERO)
		result.expectCookie().maxAge(REFRESH_TOKEN_COOKIE, Duration.ZERO)
		verify(authenticationPort).getLogoutUri(redirectUri, null, null)
	}

	@Test
	fun `Should getLogoutUri forward the refresh token cookie to be revoked`() {
		// Arrange
		val redirectUri = "redirectUri"
		whenever(authenticationPort.getLogoutUri(any(), anyOrNull(), any())).thenReturn(Mono.just(AuthenticationUriModel("uri")))

		// Act
		// Deliberately no access token cookie: any value there gets picked up by CookieBearerTokenHandler
		// as a bearer token candidate, and a fake one fails JWT validation before this permitAll route
		// is even reached. The access-token extraction is the same one-liner, covered by the "no cookies"
		// case above plus AuthenticationCookieService's own tests.
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/logout/uri", emptyList(), listOf("redirectUri" to redirectUri)))
			.cookie(REFRESH_TOKEN_COOKIE, "refreshToken")
			.exchange()

		// Assert
		result.body<AuthenticationUriModel>(OK)
		verify(authenticationPort).getLogoutUri(redirectUri, null, "refreshToken")
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
		whenever(authenticationPort.getAuthenticationToken(any(), any())).thenReturn(Mono.just(token()))

		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token", emptyList(), emptyList()))
			.bodyValue(body)
			.exchange()

		// Assert
		result.expectStatus().isOk
		result.expectCookie().httpOnly(ACCESS_TOKEN_COOKIE, true)
		result.expectCookie().value(ACCESS_TOKEN_COOKIE) { assertEquals(token().accessToken, it) }
		result.expectCookie().maxAge(ACCESS_TOKEN_COOKIE, Duration.ofSeconds(token().expiresIn))
		result.expectCookie().httpOnly(REFRESH_TOKEN_COOKIE, true)
		result.expectCookie().value(REFRESH_TOKEN_COOKIE) { assertEquals(token().refreshToken, it) }
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
		whenever(authenticationPort.refreshAuthenticationToken(any())).thenReturn(Mono.just(token()))

		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.cookie(REFRESH_TOKEN_COOKIE, "refreshToken")
			.exchange()

		// Assert
		result.expectStatus().isOk
		result.expectCookie().httpOnly(ACCESS_TOKEN_COOKIE, true)
		result.expectCookie().value(ACCESS_TOKEN_COOKIE) { assertEquals(token().accessToken, it) }
		result.expectCookie().httpOnly(REFRESH_TOKEN_COOKIE, true)
		result.expectCookie().value(REFRESH_TOKEN_COOKIE) { assertEquals(token().refreshToken, it) }
		verify(authenticationPort).refreshAuthenticationToken("refreshToken")
	}

	@Test
	fun `Should refreshToken return 401 when no refresh token cookie is sent`() {
		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(UNAUTHORIZED, REFRESH_TOKEN_OUTDATED)
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
