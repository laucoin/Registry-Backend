package fr.laucoin.registry.backend.domain.handler

import java.util.stream.Stream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.ServerWebExchange

class AuthenticationSuccessHandlerTest {
    private val handler: AuthenticationSuccessHandler = AuthenticationSuccessHandler(FRONTEND_URL)

    companion object {
        private const val FRONTEND_URL: String = "https://registry.laucoin.fr"

        @JvmStatic
        fun `Should onAuthenticationSuccess return to frontend on login succeed`(): Stream<Arguments> = Stream.of(
            Arguments.of(null),
            Arguments.of("   "),
            Arguments.of(FRONTEND_URL),
            Arguments.of("$FRONTEND_URL/test"),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should onAuthenticationSuccess return to frontend on login succeed`(
        redirectUri: String?
    ) {
        // Arrange
        val attributes = mapOf("redirectUri" to redirectUri)
        val response: ServerHttpResponse = mock()
        val exchange: ServerWebExchange = mock()
        val webFilterExchange: WebFilterExchange = mock()
        val authentication: Authentication = mock()

        `when`(exchange.attributes).thenReturn(attributes)
        `when`(response.setStatusCode(any())).thenReturn(true)
        `when`(response.headers).thenReturn(HttpHeaders())
        `when`(exchange.response).thenReturn(response)
        `when`(webFilterExchange.exchange).thenReturn(exchange)

        // Act
        val result = Assertions.assertDoesNotThrow {
            handler.onAuthenticationSuccess(webFilterExchange, authentication).block()
        }

        // Assert
        assertNotNull(result)
    }
}
