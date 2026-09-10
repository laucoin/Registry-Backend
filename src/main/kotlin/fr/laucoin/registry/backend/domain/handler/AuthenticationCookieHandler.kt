package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.model.AuthorizationChallengeModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.time.Duration

@Component
class AuthenticationCookieHandler(
	@param:Value($$"${registry.security.cookie.domain:}")
	private val domain: String,
	@param:Value($$"${registry.security.cookie.secure:true}")
	private val secure: Boolean,
	@param:Value($$"${registry.security.cookie.same-site:Lax}")
	private val sameSite: String,
) {
	private val log = LoggerFactory.getLogger(this::class.java)

	companion object {
		const val ACCESS_TOKEN_COOKIE = "registry_token"
		const val REFRESH_TOKEN_COOKIE = "registry_refresh"
		const val REFRESH_TOKEN_PATH = "/api/v1/authentication/token"
		const val STATE_COOKIE = "registry_state"
		const val CODE_VERIFIER_COOKIE = "registry_verifier"
		const val AUTHENTICATION_PATH = "/api/v1/authentication"

		private const val CHALLENGE_MAX_AGE = 600L
		private const val ROOT_PATH = "/"
		private const val STRICT = "Strict"
	}

	fun write(exchange: ServerWebExchange, token: TokenModel) {
		exchange.response.addCookie(
			cookie(ACCESS_TOKEN_COOKIE, token.accessToken, ROOT_PATH, sameSite, token.expiresIn)
		)

		if (token.refreshToken == null) {
			log.warn(
				"The provider issued no refresh token, so the session cannot be renewed and will end "
						+ "when the access token expires. Check that \"offline_access\" is among the requested "
						+ "scopes and granted by the provider."
			)
			return
		}

		exchange.response.addCookie(
			cookie(REFRESH_TOKEN_COOKIE, token.refreshToken, REFRESH_TOKEN_PATH, STRICT, token.refreshExpiresIn)
		)
	}

	fun writeChallenge(exchange: ServerWebExchange, challenge: AuthorizationChallengeModel) {
		exchange.response.addCookie(
			cookie(STATE_COOKIE, challenge.state, AUTHENTICATION_PATH, STRICT, CHALLENGE_MAX_AGE)
		)
		exchange.response.addCookie(
			cookie(CODE_VERIFIER_COOKIE, challenge.codeVerifier, AUTHENTICATION_PATH, STRICT, CHALLENGE_MAX_AGE)
		)
	}

	fun readState(exchange: ServerWebExchange): String? = read(exchange, STATE_COOKIE)

	fun readCodeVerifier(exchange: ServerWebExchange): String? = read(exchange, CODE_VERIFIER_COOKIE)

	fun clearChallenge(exchange: ServerWebExchange) {
		exchange.response.addCookie(cookie(STATE_COOKIE, "", AUTHENTICATION_PATH, STRICT, maxAge = 0L))
		exchange.response.addCookie(cookie(CODE_VERIFIER_COOKIE, "", AUTHENTICATION_PATH, STRICT, maxAge = 0L))
	}

	private fun read(exchange: ServerWebExchange, name: String): String? =
		exchange.request.cookies.getFirst(name)?.value?.takeIf { it.isNotBlank() }

	fun readRefreshToken(exchange: ServerWebExchange): String? =
		exchange.request.cookies.getFirst(REFRESH_TOKEN_COOKIE)?.value?.takeIf { it.isNotBlank() }

	fun clear(exchange: ServerWebExchange) {
		exchange.response.addCookie(cookie(ACCESS_TOKEN_COOKIE, "", ROOT_PATH, sameSite, maxAge = 0L))
		exchange.response.addCookie(cookie(REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_PATH, STRICT, maxAge = 0L))
	}

	private fun cookie(name: String, value: String, path: String, sameSite: String, maxAge: Long?): ResponseCookie {
		val builder = ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite(sameSite)
			.path(path)
			.maxAge(maxAge?.let(Duration::ofSeconds) ?: Duration.ofSeconds(-1))

		return if (domain.isBlank()) builder.build() else builder.domain(domain).build()
	}
}
