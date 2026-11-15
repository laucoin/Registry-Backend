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
 * Objectif : Calcule le token CSRF sans état et le fournit à Spring Security.
 * Périmètre : Dérive le token comme un HMAC du token d'accès présenté avec le secret client de l'IDP, de sorte que rien n'est stocké.
 * Limites : Valide uniquement pour le token d'accès dont il est dérivé.
 */
@Component
class CsrfTokenService(
	@Value($$"${external.idp.client-secret}")
	hmacSecret: String,
): ServerCsrfTokenRepository {
	private val hmacKey = SecretKeySpec(derive(hmacSecret), ALGORITHM)

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
		mac.init(hmacKey)
		val digest = mac.doFinal(value.toByteArray(UTF_8))
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
	}

	private fun derive(secret: String): ByteArray {
		val mac = Mac.getInstance(ALGORITHM)
		mac.init(SecretKeySpec(secret.toByteArray(UTF_8), ALGORITHM))
		return mac.doFinal(KEY_DERIVATION_LABEL.toByteArray(UTF_8))
	}

	companion object {
		private const val KEY_DERIVATION_LABEL = "registry-csrf-token-v1"
		const val HEADER_NAME = "X-XSRF-TOKEN"
		const val PARAMETER_NAME = "_csrf"
		private const val ALGORITHM = "HmacSHA256"
	}
}
