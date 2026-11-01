package fr.laucoin.registry.backend.domain.service.impl

import org.mockito.kotlin.anyOrNull
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_NOT_ALLOWED
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.BAD_REQUEST
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AuthenticationServiceTest {
	private val port: IAuthenticationPort = mock()
	private val service = AuthenticationService(
		port,
		configuredRedirectUris = listOf(LOGIN_URI),
		configuredLogoutRedirectUris = listOf(LOGOUT_URI),
	)

	private companion object {
		private const val LOGIN_URI = "https://registry.test/auth/callback"
		private const val LOGOUT_URI = "https://registry.test"
	}

	@Test
	fun `Should getLoginUri delegate an allowed redirect uri`() {
		// Arrange
		whenever(port.getLoginUri(LOGIN_URI, null)).thenReturn(AuthenticationUriModel("uri"))

		// Act
		val result = service.getLoginUri(LOGIN_URI, null)

		// Assert
		assertEquals("uri", result.uri)
	}

	@ParameterizedTest
	@ValueSource(
		strings = [
			"https://evil.test", "$LOGIN_URI/", "HTTPS://REGISTRY.TEST/auth/callback", "$LOGIN_URI?x=1",
			" $LOGIN_URI", "${LOGIN_URI}x", LOGOUT_URI, "$LOGIN_URI#x", "https://registry.test/auth%2Fcallback", "$LOGIN_URI\u0000",
		],
	)
	fun `Should getLoginUri refuse every redirect uri that is not an exact allowed value`(redirectUri: String) {
		// Act
		val error = assertThrows(RegistryException::class.java) { service.getLoginUri(redirectUri, null) }

		// Assert
		assertEquals(BAD_REQUEST, error.status)
		assertEquals(REDIRECT_URI_NOT_ALLOWED, error.code)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should getLogoutUri delegate an uri allowed for logout`() {
		// Arrange
		whenever(port.getLogoutUri(LOGOUT_URI, "access", "refresh")).thenReturn(Mono.just(AuthenticationUriModel("uri")))

		// Act
		val result = service.getLogoutUri(LOGOUT_URI, "access", "refresh")

		// Assert
		StepVerifier.create(result).expectNextMatches { it.uri == "uri" }.verifyComplete()
	}

	@ParameterizedTest
	@ValueSource(strings = ["https://evil.test", "$LOGOUT_URI/", LOGIN_URI])
	fun `Should getLogoutUri refuse every uri that is not allowed for logout without calling the port`(redirectUri: String) {
		// Act
		val result = service.getLogoutUri(redirectUri, "access", "refresh")

		// Assert
		StepVerifier.create(result).expectErrorMatches { it is RegistryException && it.code == REDIRECT_URI_NOT_ALLOWED }.verify()
		verifyNoInteractions(port)
	}

	@Test
	fun `Should getAuthenticationToken delegate an allowed redirect uri`() {
		// Arrange
		whenever(port.getAuthenticationToken("code", LOGIN_URI)).thenReturn(Mono.just(TokenModel("access", 300L, "refresh", "Bearer")))

		// Act
		val result = service.getAuthenticationToken("code", LOGIN_URI)

		// Assert
		StepVerifier.create(result).expectNextCount(1).verifyComplete()
	}

	@ParameterizedTest
	@ValueSource(strings = ["https://evil.test", "$LOGIN_URI/", LOGOUT_URI])
	fun `Should getAuthenticationToken refuse a redirect uri that is not allowed without calling the port`(redirectUri: String) {
		// Act
		val result = service.getAuthenticationToken("code", redirectUri)

		// Assert
		StepVerifier.create(result).expectErrorMatches { it is RegistryException && it.code == REDIRECT_URI_NOT_ALLOWED }.verify()
		verifyNoInteractions(port)
	}

	@Test
	fun `Should refreshAuthenticationToken delegate to the port`() {
		// Arrange
		whenever(port.refreshAuthenticationToken(any())).thenReturn(Mono.just(TokenModel("access", 300L, "refresh", "Bearer")))

		// Act
		val result = service.refreshAuthenticationToken("refresh")

		// Assert
		StepVerifier.create(result).expectNextCount(1).verifyComplete()
		verify(port).refreshAuthenticationToken("refresh")
	}

	@Test
	fun `Should refuse to start without any allowed redirect uri`() {
		// Act
		val error = assertThrows(IllegalArgumentException::class.java) {
			AuthenticationService(port, configuredRedirectUris = emptyList(), configuredLogoutRedirectUris = listOf(LOGOUT_URI))
		}

		// Assert
		assertEquals("external.idp.allowed-redirect-uris must not be empty", error.message)
	}

	@Test
	fun `Should refuse to start without any allowed logout redirect uri`() {
		// Act
		val error = assertThrows(IllegalArgumentException::class.java) {
			AuthenticationService(port, configuredRedirectUris = listOf(LOGIN_URI), configuredLogoutRedirectUris = emptyList())
		}

		// Assert
		assertEquals("external.idp.allowed-logout-redirect-uris must not be empty", error.message)
	}

	@Test
	fun `Should getLogoutUri propagate a failure of the port after an allowed uri`() {
		// Arrange
		whenever(port.getLogoutUri(any(), any(), any())).thenReturn(Mono.error(IllegalStateException("idp down")))

		// Act
		val result = service.getLogoutUri(LOGOUT_URI, "access", "refresh")

		// Assert
		StepVerifier.create(result).expectError(IllegalStateException::class.java).verify()
	}

	@Test
	fun `Should match an entry ending with a slash exactly`() {
		// Arrange
		val strictService = AuthenticationService(
			port,
			configuredRedirectUris = listOf("$LOGIN_URI/", " ", "https://registry.test/*"),
			configuredLogoutRedirectUris = listOf(LOGOUT_URI),
		)
		whenever(port.getLoginUri(any(), anyOrNull())).thenReturn(AuthenticationUriModel("uri"))

		// Act
		val allowed = strictService.getLoginUri("$LOGIN_URI/", null)
		val refused = assertThrows(RegistryException::class.java) { strictService.getLoginUri("https://registry.test/x", null) }

		// Assert
		assertEquals("uri", allowed.uri)
		assertEquals(REDIRECT_URI_NOT_ALLOWED, refused.code)
	}
}
