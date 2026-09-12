package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
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
 * Computes a CSRF token deterministically as `HMAC-SHA256(secret, accessToken)` instead of a
 * random value stored server-side or in a cookie. The backend stays fully stateless — the expected
 * token is simply recomputed from whichever access token the caller already presents — and the
 * value is handed to the client via a plain response header (see `CsrfTokenHeaderHandler`) rather
 * than a cookie, which sidesteps the double-submit-cookie pattern's requirement that front and
 * back share a cookie-readable domain (not guaranteed across every environment this app runs in).
 *
 * Reuses the IDP client secret as the HMAC key rather than introducing a dedicated one: it's
 * already a managed secret, HMAC is one-way so this use doesn't expose it, and it stays valid
 * across restarts and multiple instances without new required configuration.
 *
 * Doubles as the [ServerCsrfTokenRepository] Spring's `csrf {}` DSL wires in: `generateToken` and
 * `loadToken` recompute the same deterministic value (there's no "new" vs "existing" when the
 * token is a pure function of the caller's access token rather than server-stored state), and
 * `saveToken` is a no-op since there's nothing to persist.
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
