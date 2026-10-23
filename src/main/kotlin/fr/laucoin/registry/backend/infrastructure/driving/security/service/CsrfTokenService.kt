package fr.laucoin.registry.backend.infrastructure.driving.security.service

import fr.laucoin.registry.backend.infrastructure.driving.security.service.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.web.server.csrf.CsrfToken
import org.springframework.security.web.server.csrf.DefaultCsrfToken
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Purpose: Computes the stateless CSRF token and serves it to Spring Security.
 * Scope: Derives the token as an HMAC of the presented access token with the IDP client secret, so nothing is stored.
 * Limits: Valid only for the access token it was derived from.
 */
@Component
class CsrfTokenService(
	@param:Value($$"${external.idp.client-secret}")
	private val hmacSecret: String,
): ServerCsrfTokenRepository {
	fun computeToken(exchange: ServerWebExchange): String = hmac(extractBearer(exchange).orEmpty())

	override fun generateToken(exchange: ServerWebExchange): Mono<CsrfToken> = token(exchange)

	override fun saveToken(exchange: ServerWebExchange, token: CsrfToken?): Mono<Void> = Mono.empty()

	override fun loadToken(exchange: ServerWebExchange): Mono<CsrfToken> = token(exchange)

	private fun token(exchange: ServerWebExchange): Mono<CsrfToken> =
		Mono.fromSupplier { DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, computeToken(exchange)) }

	private fun extractBearer(exchange: ServerWebExchange): String? {
		val header = exchange.request.headers.getFirst(AUTHORIZATION)
		if (header?.startsWith("Bearer ") == true) return header.removePrefix("Bearer ")
		return exchange.request.cookies.getFirst(ACCESS_TOKEN_COOKIE)?.value
	}

	private fun hmac(value: String): String {
		val mac = Mac.getInstance(ALGORITHM)
		mac.init(SecretKeySpec(hmacSecret.toByteArray(UTF_8), ALGORITHM))
		val digest = mac.doFinal(value.toByteArray(UTF_8))
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
	}

	companion object {
		const val HEADER_NAME = "X-XSRF-TOKEN"
		const val PARAMETER_NAME = "_csrf"
		private const val ALGORITHM = "HmacSHA256"
	}
}
