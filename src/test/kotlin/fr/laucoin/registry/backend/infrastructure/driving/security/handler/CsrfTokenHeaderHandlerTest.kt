package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.infrastructure.driving.security.service.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService.Companion.HEADER_NAME
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.http.HttpMethod.GET
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class CsrfTokenHeaderHandlerTest {
	private val tokenService = CsrfTokenService(hmacSecret = "test-secret")
	private val handler = CsrfTokenHeaderHandler(tokenService)
	private val chainCalls = AtomicInteger(0)
	private val chain = WebFilterChain {
		chainCalls.incrementAndGet()
		Mono.empty()
	}

	@Test
	fun `Should filter set the computed token as a response header and call the chain`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.method(GET, "/api/v2/whatever").cookie(HttpCookie(ACCESS_TOKEN_COOKIE, "accessToken"))
		)

		// Act
		handler.filter(exchange, chain).block()

		// Assert
		assertEquals(1, chainCalls.get())
		assertEquals(tokenService.computeToken(exchange), exchange.response.headers.getFirst(HEADER_NAME))
	}
}
