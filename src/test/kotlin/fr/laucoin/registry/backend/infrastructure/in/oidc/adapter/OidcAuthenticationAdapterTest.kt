package fr.laucoin.registry.backend.infrastructure.`in`.oidc.adapter

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_OUTDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_PROVIDER_FAILED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.`in`.oidc.mapper.AuthenticationTokenEntityMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.spy
import org.springframework.http.HttpStatus.FAILED_DEPENDENCY
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.util.ReflectionTestUtils.setField
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.Exceptions
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OidcAuthenticationAdapterTest {
	private val mockWebServer: MockWebServer = MockWebServer()
	private val mapper: AuthenticationTokenEntityMapper = spy()
	private val adapter: OidcAuthenticationAdapter = OidcAuthenticationAdapter(
		mapper,
		authorizationUri = "authorizationUri",
		tokenUri = "tokenUri",
		endSessionUri = "endSessionUri",
		clientId = "clientId",
		clientSecret = "clientSecret",
	)

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
		val redirectUri = "redirectUri"
		val expected =
			"${mockWebServer.url("/protocol/openid-connect/auth")}?response_type=code&client_id=clientId&redirect_uri=redirectUri"

		// Act
		val result = adapter.getLoginUri(redirectUri)

		// Assert
		assertNotNull(result)
		assertEquals(expected, result.uri)
	}

	@Test
	fun `Should getLogoutUri return built logout url`() {
		// Arrange
		val redirectUri = "redirectUri"
		val expected = "${mockWebServer.url("/protocol/openid-connect/logout")}?redirect_uri=redirectUri"

		// Act
		val result = adapter.getLogoutUri(redirectUri)

		// Assert
		assertNotNull(result)
		assertEquals(expected, result.uri)
	}

	@Test
	fun `Should getAuthenticationToken call the IdP to fetch token 2xx`() {
		// Arrange
		val redirectUri = "redirectUri"
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

	@Test
	fun `Should getAuthenticationToken call the IdP to fetch token 4xx`() {
		// Arrange
		val redirectUri = "redirectUri"
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
	fun `Should getAuthenticationToken call the IdP to fetch token 5xx`() {
		// Arrange
		val redirectUri = "redirectUri"
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
	fun `Should refreshAuthenticationToken call the IdP to refresh token 2xx`() {
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
	fun `Should refreshAuthenticationToken call the IdP to refresh token 4xx`() {
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
	fun `Should refreshAuthenticationToken call the IdP to refresh token 5xx`() {
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
