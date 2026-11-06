package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import fr.laucoin.registry.backend.infrastructure.driving.security.service.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService.Companion.HEADER_NAME
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.ETAG
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.HttpHeaders.ORIGIN
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient

class SecurityChainIntegrationTest : TestContext() {
	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		const val PROTECTED_WRITE = "/api/v2/projects"
		const val PUBLIC_READ = "/api/v2/metadata/movements/types"
		const val ALLOWED_ORIGIN = "https://test.frontend.com"
		const val FOREIGN_ORIGIN = "https://evil.example"
		const val API_HOST = "https://localhost"
	}

	@Test
	fun `Should refuse a cookie authenticated write carrying a forged csrf token`() {
		// Act
		val result = webClient
			.post()
			.uri(PROTECTED_WRITE)
			.cookie(ACCESS_TOKEN_COOKIE, "stolen-session")
			.header(HEADER_NAME, "forged")
			.contentType(APPLICATION_JSON)
			.bodyValue("{}")
			.exchange()

		// Assert
		result.expectStatus().isForbidden
	}

	@Test
	fun `Should let a write carrying the expected csrf token reach authentication`() {
		// Act
		val result = webClient
			.post()
			.uri(PROTECTED_WRITE)
			.contentType(APPLICATION_JSON)
			.bodyValue("{}")
			.exchange()

		// Assert
		result.expectStatus().isUnauthorized
	}

	@Test
	fun `Should skip the csrf check for a write carrying a bearer token`() {
		// Act
		val result = webClient
			.post()
			.uri(PROTECTED_WRITE)
			.header(HEADER_NAME, "forged")
			.header(AUTHORIZATION, "Bearer not-a-jwt")
			.contentType(APPLICATION_JSON)
			.bodyValue("{}")
			.exchange()

		// Assert
		result.expectStatus().isUnauthorized
	}

	@Test
	fun `Should skip the csrf check on the token exchange endpoint`() {
		// Act
		val result = webClient
			.post()
			.uri("/api/v2/authentication/token")
			.header(HEADER_NAME, "forged")
			.contentType(APPLICATION_JSON)
			.bodyValue("{}")
			.exchange()

		// Assert
		result.expectStatus().isBadRequest
	}

	@Test
	fun `Should write the defensive security headers on every api response`() {
		// Act
		val result = webClient
			.get()
			.uri("$API_HOST$PUBLIC_READ")
			.exchange()

		// Assert
		result.expectStatus().isOk
		result.expectHeader().valueEquals("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
		result.expectHeader().valueEquals(
			"Permissions-Policy",
			"geolocation=(), camera=(), microphone=(), payment=(), usb=(), interest-cohort=()",
		)
		result.expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains ; preload")
		result.expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
		result.expectHeader().valueEquals("X-Frame-Options", "DENY")
	}

	@Test
	fun `Should allow a cors preflight from an allowed origin`() {
		// Act
		val result = webClient
			.method(OPTIONS)
			.uri("$API_HOST$PROTECTED_WRITE")
			.header(ORIGIN, ALLOWED_ORIGIN)
			.header(ACCESS_CONTROL_REQUEST_METHOD, POST.name())
			.exchange()

		// Assert
		result.expectStatus().isOk
		result.expectHeader().valueEquals(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN)
		result.expectHeader().valueEquals(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
	}

	@Test
	fun `Should refuse a cors preflight from a foreign origin`() {
		// Act
		val result = webClient
			.method(OPTIONS)
			.uri("$API_HOST$PROTECTED_WRITE")
			.header(ORIGIN, FOREIGN_ORIGIN)
			.header(ACCESS_CONTROL_REQUEST_METHOD, POST.name())
			.exchange()

		// Assert
		result.expectStatus().isForbidden
		result.expectHeader().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN)
	}

	@Test
	fun `Should expose only the csrf, etag and location headers to an allowed origin`() {
		// Act
		val result = webClient
			.get()
			.uri("$API_HOST$PUBLIC_READ")
			.header(ORIGIN, ALLOWED_ORIGIN)
			.exchange()

		// Assert
		result.expectStatus().isOk
		result.expectHeader().values(ACCESS_CONTROL_EXPOSE_HEADERS) { exposed ->
			val names = exposed.flatMap { it.split(',') }.map { it.trim() }.toSet()
			assertEquals(setOf(HEADER_NAME, ETAG, LOCATION), names)
		}
		result.expectHeader().value(HEADER_NAME) { assertTrue(it.isNotBlank()) }
	}
}
