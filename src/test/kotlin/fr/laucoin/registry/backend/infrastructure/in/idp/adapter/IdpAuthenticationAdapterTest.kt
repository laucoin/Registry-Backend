package fr.laucoin.registry.backend.infrastructure.`in`.idp.adapter

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_OUTDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_PROVIDER_FAILED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.`in`.idp.mapper.AuthenticationTokenEntityMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

class IdpAuthenticationAdapterTest {
	private val mockWebServer: MockWebServer = MockWebServer()
	private val mapper: AuthenticationTokenEntityMapper = spy()
	private val adapter: IdpAuthenticationAdapter = IdpAuthenticationAdapter(
		mapper,
		authorizationUri = "authorizationUri",
		tokenUri = "tokenUri",
		endSessionUri = "endSessionUri",
		revocationUri = "revocationUri",
		clientId = "clientId",
		clientSecret = "clientSecret",
	)

	@BeforeEach
	fun setUp() {
		mockWebServer.start()
		setField(adapter, "http", WebClient.create())
		setField(adapter, "authorizationUri", mockWebServer.url("/oauth2/authorize").toString())
		setField(adapter, "tokenUri", mockWebServer.url("/oauth2/token").toString())
		setField(adapter, "endSessionUri", mockWebServer.url("/oauth2/logout").toString())
		setField(adapter, "revocationUri", mockWebServer.url("/oauth2/revoke").toString())
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
			"${mockWebServer.url("/oauth2/authorize")}?response_type=code&client_id=clientId&redirect_uri=redirectUri"

		// Act
		val result = adapter.getLoginUri(redirectUri)

		// Assert
		assertNotNull(result)
		assertEquals(expected, result.uri)
	}

	@Test
	fun `Should getLogoutUri return built logout url without calling revocation when no token is provided`() {
		// Arrange
		val redirectUri = "redirectUri"
		val expected = "${mockWebServer.url("/oauth2/logout")}?redirect_uri=redirectUri"

		// Act
		val result = adapter.getLogoutUri(redirectUri, accessToken = null, refreshToken = null).block()

		// Assert
		assertNotNull(result)
		assertEquals(expected, result.uri)
		assertEquals(0, mockWebServer.requestCount)
	}

	@Test
	fun `Should getLogoutUri revoke both the access and refresh token when provided`() {
		// Arrange
		val redirectUri = "redirectUri"
		mockWebServer.enqueue(MockResponse().setResponseCode(200))
		mockWebServer.enqueue(MockResponse().setResponseCode(200))

		// Act
		val result = adapter.getLogoutUri(redirectUri, accessToken = "accessToken", refreshToken = "refreshToken").block()

		// Assert
		assertNotNull(result)
		assertEquals(2, mockWebServer.requestCount)
		val bodies = listOf(mockWebServer.takeRequest().body.readUtf8(), mockWebServer.takeRequest().body.readUtf8())
		assertEquals(true, bodies.any { it.contains("token=accessToken") && it.contains("token_type_hint=access_token") })
		assertEquals(true, bodies.any { it.contains("token=refreshToken") && it.contains("token_type_hint=refresh_token") })
	}

	@Test
	fun `Should getLogoutUri still succeed when revocation returns 4xx`() {
		// Arrange
		val redirectUri = "redirectUri"
		mockWebServer.enqueue(MockResponse().setResponseCode(400))

		// Act
		val result = adapter.getLogoutUri(redirectUri, accessToken = "accessToken", refreshToken = null).block()

		// Assert
		assertNotNull(result)
	}

	@Test
	fun `Should getLogoutUri still succeed when revocation returns 5xx`() {
		// Arrange
		val redirectUri = "redirectUri"
		mockWebServer.enqueue(MockResponse().setResponseCode(500))

		// Act
		val result = adapter.getLogoutUri(redirectUri, accessToken = "accessToken", refreshToken = null).block()

		// Assert
		assertNotNull(result)
	}


	@Test
	fun `Should getAuthenticationToken call the IDP to fetch token 2xx`() {
		// Arrange
		val redirectUri = "redirectUri"
		val authorizationCode = "authorizationCode"

		val responseBody = """{
            "access_token": "accessToken",
            "refresh_token": "refreshToken",
            "expires_in": 3600,
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
	}

	@Test
	fun `Should getAuthenticationToken call the IDP to fetch token 4xx`() {
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
	fun `Should getAuthenticationToken call the IDP to fetch token 5xx`() {
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
	fun `Should refreshAuthenticationToken call the IDP to refresh token 2xx`() {
		// Arrange
		val refreshToken = "refreshToken"

		val responseBody = """{
            "access_token": "accessToken",
            "refresh_token": "refreshToken",
            "expires_in": 3600,
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
	}

	@Test
	fun `Should refreshAuthenticationToken call the IDP to refresh token 4xx`() {
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
	fun `Should refreshAuthenticationToken call the IDP to refresh token 5xx`() {
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
