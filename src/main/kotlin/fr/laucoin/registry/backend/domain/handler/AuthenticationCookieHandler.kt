package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.model.AuthorizationChallengeModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange

/**
 * Writes and clears the cookies that carry a session.
 *
 * Both tokens live in `HttpOnly` cookies rather than in browser storage, so that a script running in
 * the origin — the application's own, or any of its dependencies — has nothing to read. See
 * [ADR 013](https://doc.laucoin.fr/registry/technical/adr/013-cookie-session-transport).
 *
 * The refresh cookie is confined to the refresh path: it is the renewable credential, and there is no
 * reason for it to travel with ordinary API traffic.
 */
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

		/** The refresh cookie is sent to `/token` and `/token/refresh`, and nowhere else. */
		const val REFRESH_TOKEN_PATH = "/api/v1/authentication/token"

		const val STATE_COOKIE = "registry_state"
		const val CODE_VERIFIER_COOKIE = "registry_verifier"

		/** The challenge cookies are only ever read back by the token exchange. */
		const val AUTHENTICATION_PATH = "/api/v1/authentication"

		/**
		 * How long a sign-in may take. Long enough for a password manager, a second factor and a
		 * moment of hesitation; short enough that an abandoned attempt stops being usable.
		 */
		private const val CHALLENGE_MAX_AGE = 600L

		private const val ROOT_PATH = "/"

		/**
		 * The refresh cookie is never sent on a cross-site request, whatever the access cookie is
		 * configured to do: nothing legitimately navigates to the refresh endpoint from elsewhere.
		 */
		private const val STRICT = "Strict"
	}

	fun write(exchange: ServerWebExchange, token: TokenModel) {
		exchange.response.addCookie(
			cookie(ACCESS_TOKEN_COOKIE, token.accessToken, ROOT_PATH, sameSite, token.expiresIn)
		)

		if (token.refreshToken == null) {
			// Nothing to renew with. The session still works until the access token expires, so this
			// is a warning rather than a failure — but it is almost always a missing `offline_access`
			// scope, and without this line the symptom is an unexplained sign-out later on.
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

	/**
	 * Remembers the challenge for the length of the round trip to the provider.
	 *
	 * Held in cookies rather than server-side so the backend stays stateless and survives a restart
	 * mid-login. `Strict` throughout: nothing legitimately arrives at the token exchange from another
	 * site, and these two values are precisely what an attacker would need to forge one.
	 */
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

	/**
	 * Expires the challenge cookies. A challenge is good for exactly one exchange, so they go whether
	 * it succeeded or failed — a verifier left behind is a replay waiting to happen.
	 */
	fun clearChallenge(exchange: ServerWebExchange) {
		exchange.response.addCookie(cookie(STATE_COOKIE, "", AUTHENTICATION_PATH, STRICT, maxAge = 0L))
		exchange.response.addCookie(cookie(CODE_VERIFIER_COOKIE, "", AUTHENTICATION_PATH, STRICT, maxAge = 0L))
	}

	private fun read(exchange: ServerWebExchange, name: String): String? =
		exchange.request.cookies.getFirst(name)?.value?.takeIf { it.isNotBlank() }

	/** The refresh token as the browser sent it, or `null` when there is no usable session. */
	fun readRefreshToken(exchange: ServerWebExchange): String? =
		exchange.request.cookies.getFirst(REFRESH_TOKEN_COOKIE)?.value?.takeIf { it.isNotBlank() }

	/**
	 * Expires both cookies. A browser only drops a cookie when the replacement matches its name,
	 * domain **and** path, so the attributes here have to mirror [write] exactly.
	 */
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
			// An unknown lifetime makes it a session cookie, dropped when the browser closes. That is
			// what sessionStorage did before this change, so nothing regresses — and it beats inventing
			// an expiry the provider never stated.
			.maxAge(maxAge?.let(Duration::ofSeconds) ?: Duration.ofSeconds(-1))

		// An empty domain would be written out as `Domain=`, which browsers reject. Leaving the
		// attribute off makes the cookie host-only, which is what local development wants.
		return if (domain.isBlank()) builder.build() else builder.domain(domain).build()
	}
}
