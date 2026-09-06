package fr.laucoin.registry.backend.infrastructure.`in`.keycloak.adapter

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_NOT_ALLOWED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_OUTDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_PROVIDER_FAILED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.`in`.keycloak.mapper.AuthenticationTokenEntityMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.spy
import org.springframework.http.HttpStatus.FAILED_DEPENDENCY
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.test.util.ReflectionTestUtils.setField
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.Exceptions

class KeycloakAuthenticationAdapterTest {
	private val mockWebServer: MockWebServer = MockWebServer()
	private val mapper: AuthenticationTokenEntityMapper = spy()
	private val adapter: KeycloakAuthenticationAdapter = KeycloakAuthenticationAdapter(
		mapper,
		authorizationUri = "authorizationUri",
		tokenUri = "tokenUri",
		endSessionUri = "endSessionUri",
		clientId = "clientId",
		clientSecret = "clientSecret",
		allowedOrigins = listOf(ALLOWED_ORIGIN),
	)

	private companion object {
		private const val ALLOWED_ORIGIN = "https://app.test"
		private const val REDIRECT_URI = "$ALLOWED_ORIGIN/auth/callback"
	}

	@BeforeEach
	fun setUp() {
		mockWebServer.start()
		setField(adapter, "http", WebClient.create())
		setField(adapter, "authorizationUri", mockWebServer.url("/protocol/openid-connect/auth").toString())
		setField(adapter, "tokenUri", mockWebServer.url("/protocol/openid-connect/token").toString())
		setField(adapter, "endSessionUri", mockWebServer.url("/protocol/openid-connect/logout").toString())
	}

	@AfterEach
	fun tearDown() {
		mockWebServer.shutdown()
	}

	@Test
	fun `Should getLoginUri return built auth url`() {
		// Arrange
		val redirectUri = REDIRECT_URI
		val expected =
			"${mockWebServer.url("/protocol/openid-connect/auth")}?response_type=code&client_id=clientId" +
				"&scope=openid%20profile%20email%20offline_access" +
				"&redirect_uri=https://app.test/auth/callback"

		// Act
		val result = adapter.getLoginUri(redirectUri)

		// Assert
		assertNotNull(result)
		assertEquals(expected, result.uri)
	}

	/**
	 * The redirect used to be concatenated into the URL unencoded, so an `&` in it added parameters
	 * of the caller's choosing to the authorization request. Building through UriComponentsBuilder
	 * escapes it, and the whole value stays one parameter.
	 */
	@Test
	fun `Should not let a redirect URI inject extra authorization parameters`() {
		// Arrange
		val hostile = "$ALLOWED_ORIGIN/auth/callback?a=1&prompt=none&client_id=other"

		// Act
		val result = adapter.getLoginUri(hostile)

		// Assert
		assertEquals(1, result.uri.split("client_id=").size - 1, "client_id was smuggled in a second time")
		assertFalse(result.uri.contains("&prompt=none"), "prompt was smuggled in as its own parameter")
		assertTrue(result.uri.contains("%26prompt%3Dnone"), "the ampersand should have been escaped")
	}

	@Test
	fun `Should refuse a redirect URI from another origin`() {
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			adapter.getLoginUri("https://evil.test/auth/callback")
		}) as RegistryException

		assertEquals(BAD_REQUEST, result.status)
		assertEquals(REDIRECT_URI_NOT_ALLOWED, result.message)
	}

	@Test
	fun `Should refuse a redirect URI carrying no origin at all`() {
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			adapter.getLogoutUri("/auth/callback")
		}) as RegistryException

		assertEquals(REDIRECT_URI_NOT_ALLOWED, result.message)
	}

	@Test
	fun `Should getLogoutUri return built logout url`() {
		// Arrange
		val redirectUri = REDIRECT_URI
		val expected =
			"${mockWebServer.url("/protocol/openid-connect/logout")}?redirect_uri=https://app.test/auth/callback"

		// Act
		val result = adapter.getLogoutUri(redirectUri)

		// Assert
		assertNotNull(result)
		assertEquals(expected, result.uri)
	}

	@Test
	fun `Should getAuthenticationToken call keycloak to fetch token 2xx`() {
		// Arrange
		val redirectUri = REDIRECT_URI
		val authorizationCode = "authorizationCode"

		val responseBody = """{
            "access_token": "accessToken",
            "refresh_token": "refreshToken",
            "expires_in": 3600,
            "refresh_expires_in": 18000,
            "token_type": "Bearer"
        }"""
		mockWebServer.enqueue(
			MockResponse()
				.setBody(responseBody)
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
		)

		// Act
		val result = adapter.getAuthenticationToken(authorizationCode, redirectUri).block()

		// Assert
		assertNotNull(result)
		assertEquals("Bearer", result.tokenType)
		assertEquals("accessToken", result.accessToken)
		assertEquals("refreshToken", result.refreshToken)
		assertEquals(3600, result.expiresIn)
		assertEquals(18000, result.refreshExpiresIn)
	}

	/**
	 * The response as Authentik actually sends it. Its `views/token.py` builds the body from
	 * `access_token`, `token_type`, `scope`, `expires_in` and `id_token`, and adds `refresh_token`
	 * only when `offline_access` was requested — `refresh_expires_in` is a Keycloak field it never
	 * emits. Declaring either as required made every exchange fail to decode, which is why this
	 * fixture is verbatim rather than tidied up.
	 */
	@Test
	fun `Should accept a token response carrying neither refresh token nor refresh lifetime`() {
		// Arrange
		val responseBody = """{
            "access_token": "accessToken",
            "token_type": "Bearer",
            "scope": "openid profile email",
            "expires_in": 3600,
            "id_token": "anIdToken"
        }"""
		mockWebServer.enqueue(
			MockResponse()
				.setBody(responseBody)
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
		)

		// Act
		val result = adapter.getAuthenticationToken("authorizationCode", REDIRECT_URI).block()

		// Assert
		assertNotNull(result)
		assertEquals("accessToken", result.accessToken)
		assertEquals(3600, result.expiresIn)
		assertNull(result.refreshToken)
		assertNull(result.refreshExpiresIn)
	}

	@Test
	fun `Should getAuthenticationToken call keycloak to fetch token 4xx`() {
		// Arrange
		val redirectUri = REDIRECT_URI
		val authorizationCode = "authorizationCode"

		mockWebServer.enqueue(
			MockResponse()
				.setResponseCode(400)
		)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			adapter.getAuthenticationToken(authorizationCode, redirectUri).block()
		}) as RegistryException

		// Assert
		assertNotNull(result)
		assertEquals(UNAUTHORIZED, result.status)
		assertEquals(AUTHORIZATION_CODE_OUTDATED, result.message)
	}

	@Test
	fun `Should getAuthenticationToken call keycloak to fetch token 5xx`() {
		// Arrange
		val redirectUri = REDIRECT_URI
		val authorizationCode = "authorizationCode"

		mockWebServer.enqueue(
			MockResponse()
				.setResponseCode(500)
		)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			adapter.getAuthenticationToken(authorizationCode, redirectUri).block()
		}) as RegistryException

		// Assert
		assertNotNull(result)
		assertEquals(FAILED_DEPENDENCY, result.status)
		assertEquals(AUTH_PROVIDER_FAILED, result.message)
	}

	@Test
	fun `Should refreshAuthenticationToken call keycloak to refresh token 2xx`() {
		// Arrange
		val refreshToken = "refreshToken"

		val responseBody = """{
            "access_token": "accessToken",
            "refresh_token": "refreshToken",
            "expires_in": 3600,
            "refresh_expires_in": 18000,
            "token_type": "Bearer"
        }"""
		mockWebServer.enqueue(
			MockResponse()
				.setBody(responseBody)
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
		)

		// Act
		val result = adapter.refreshAuthenticationToken(refreshToken).block()

		// Assert
		assertNotNull(result)
		assertEquals("Bearer", result.tokenType)
		assertEquals("accessToken", result.accessToken)
		assertEquals("refreshToken", result.refreshToken)
		assertEquals(3600, result.expiresIn)
		assertEquals(18000, result.refreshExpiresIn)
	}

	@Test
	fun `Should refreshAuthenticationToken call keycloak to refresh token 4xx`() {
		// Arrange
		val refreshToken = "refreshToken"

		mockWebServer.enqueue(
			MockResponse()
				.setResponseCode(400)
		)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			adapter.refreshAuthenticationToken(refreshToken).block()
		}) as RegistryException

		// Assert
		assertNotNull(result)
		assertEquals(UNAUTHORIZED, result.status)
		assertEquals(REFRESH_TOKEN_OUTDATED, result.message)
	}

	@Test
	fun `Should refreshAuthenticationToken call keycloak to refresh token 5xx`() {
		// Arrange
		val refreshToken = "refreshToken"

		mockWebServer.enqueue(
			MockResponse()
				.setResponseCode(500)
		)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			adapter.refreshAuthenticationToken(refreshToken).block()
		}) as RegistryException

		// Assert
		assertNotNull(result)
		assertEquals(FAILED_DEPENDENCY, result.status)
		assertEquals(AUTH_PROVIDER_FAILED, result.message)
	}
}
