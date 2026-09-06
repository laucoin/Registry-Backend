package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_COOKIE_MISSING
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.STATE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.STATE_MISMATCH
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.ACCESS_TOKEN_COOKIE
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.AUTHENTICATION_PATH
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.CODE_VERIFIER_COOKIE
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.REFRESH_TOKEN_COOKIE
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.REFRESH_TOKEN_PATH
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.STATE_COOKIE
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.SessionReaderDto
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.Duration
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
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
		private const val STATE = "aState"
		private const val CODE_VERIFIER = "aVerifier"

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
				Arguments.of("redirectUri", null, STATE, AUTHORIZATION_CODE_BLANK),
				Arguments.of("redirectUri", "", STATE, AUTHORIZATION_CODE_BLANK),
				Arguments.of(null, "code", STATE, REDIRECT_URI_BLANK),
				Arguments.of("", "code", STATE, REDIRECT_URI_BLANK),
				Arguments.of("redirectUri", "code", null, STATE_BLANK),
				Arguments.of("redirectUri", "code", "", STATE_BLANK),
			)
		}

	}

	@Test
	fun `Should getLoginUri return 200`() {
		// Arrange
		val redirectUri = "redirectUri"
		whenever(authenticationPort.getLoginUri(any(), any())).thenReturn(AuthenticationUriModel("uri"))

		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/login/uri", emptyList(), listOf("redirectUri" to redirectUri)))
			.exchange()

		// Assert
		result.body<AuthenticationUriModel>(OK)
		assertChallengeCookies(result)
		verify(authenticationPort).getLoginUri(eq(redirectUri), any())
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
		assertExpiredSessionCookies(result)
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
			state = STATE,
		)
		whenever(authenticationPort.getAuthenticationToken(any(), any(), any())).thenReturn(
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
			.cookie(STATE_COOKIE, STATE)
			.cookie(CODE_VERIFIER_COOKIE, CODE_VERIFIER)
			.bodyValue(body)
			.exchange()

		// Assert
		val session = result.body<SessionReaderDto>(OK)
		assertEquals(3600, session?.expiresIn)
		assertEquals(18000, session?.refreshExpiresIn)
		assertSessionCookies(result)
		verify(authenticationPort).getAuthenticationToken(body.authorizationCode!!, body.redirectUri!!, CODE_VERIFIER)
	}

	/**
	 * The whole point of `state`: a callback that did not come from a sign-in this browser started is
	 * refused, so a code obtained by someone else cannot be walked through a victim's session.
	 */
	@Test
	fun `Should fetchToken refuse a state that does not match the browser`() {
		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token", emptyList(), emptyList()))
			.cookie(STATE_COOKIE, STATE)
			.cookie(CODE_VERIFIER_COOKIE, CODE_VERIFIER)
			.bodyValue(AuthenticationInfoModel("redirectUri", "code", state = "someone-elses-state"))
			.exchange()

		// Assert
		result.assertError(UNAUTHORIZED, STATE_MISMATCH)
		verifyNoInteractions(authenticationPort)
	}

	@Test
	fun `Should fetchToken refuse a callback with no challenge cookie at all`() {
		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token", emptyList(), emptyList()))
			.bodyValue(AuthenticationInfoModel("redirectUri", "code", state = STATE))
			.exchange()

		// Assert
		result.assertError(UNAUTHORIZED, STATE_MISMATCH)
		verifyNoInteractions(authenticationPort)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should fetchToken return 400`(
		redirectUri: String?,
		authorizationCode: String?,
		state: String?,
		expectedCode: String,
	) {
		// Arrange
		val body = AuthenticationInfoModel(redirectUri, authorizationCode, state)

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
			.mutateWith(csrf())
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.cookie(REFRESH_TOKEN_COOKIE, "refreshToken")
			.exchange()

		// Assert
		val session = result.body<SessionReaderDto>(OK)
		assertEquals(3600, session?.expiresIn)
		assertSessionCookies(result)
		verify(authenticationPort).refreshAuthenticationToken("refreshToken")
	}

	/**
	 * The renewal credential is now the cookie, so a request without it is not a malformed request
	 * but an expired session — hence 401 where the empty request body used to give 400.
	 */
	@Test
	fun `Should refreshToken return 401 without the refresh cookie`() {
		// Act
		val result = webClient
			.mutateWith(csrf())
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(UNAUTHORIZED, REFRESH_COOKIE_MISSING)
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

	/**
	 * The challenge must be remembered somewhere the callback can prove it came from here, and be out
	 * of reach of script so a successful XSS cannot mint a matching `state` of its own.
	 */
	private fun assertChallengeCookies(result: WebTestClient.ResponseSpec) {
		val cookies = result.returnResult(String::class.java).responseCookies
		val state = cookies.getFirst(STATE_COOKIE)!!
		val verifier = cookies.getFirst(CODE_VERIFIER_COOKIE)!!

		assertTrue(state.isHttpOnly)
		assertTrue(verifier.isHttpOnly)
		assertEquals("Strict", state.sameSite)
		assertEquals(AUTHENTICATION_PATH, state.path)
		assertEquals(AUTHENTICATION_PATH, verifier.path)
	}

	/**
	 * A browser drops a cookie only when the replacement matches its name, domain **and** path, so a
	 * sign-out that clears them on the wrong path leaves the session alive while looking like it
	 * worked. Asserting the paths is the only way to see the difference.
	 */
	private fun assertExpiredSessionCookies(result: WebTestClient.ResponseSpec) {
		val cookies = result.returnResult(String::class.java).responseCookies
		val access = cookies.getFirst(ACCESS_TOKEN_COOKIE)!!
		val refresh = cookies.getFirst(REFRESH_TOKEN_COOKIE)!!

		assertEquals(Duration.ZERO, access.maxAge)
		assertEquals(Duration.ZERO, refresh.maxAge)
		assertEquals("", access.value)
		assertEquals("", refresh.value)
		assertEquals("/", access.path)
		assertEquals(REFRESH_TOKEN_PATH, refresh.path)
	}

	/**
	 * Both cookies must be out of reach of any script, and the refresh one confined to its own path.
	 * These attributes are the whole point of the change and nothing else in the response shows them.
	 */
	private fun assertSessionCookies(result: WebTestClient.ResponseSpec) {
		val cookies = result.returnResult(String::class.java).responseCookies
		val access = cookies.getFirst(ACCESS_TOKEN_COOKIE)!!
		val refresh = cookies.getFirst(REFRESH_TOKEN_COOKIE)!!

		assertTrue(access.isHttpOnly)
		assertTrue(refresh.isHttpOnly)
		assertEquals("/", access.path)
		assertEquals(REFRESH_TOKEN_PATH, refresh.path)
		assertEquals("Strict", refresh.sameSite)
	}
}
