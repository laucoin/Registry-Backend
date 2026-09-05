package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.model.TokenModel
import java.time.Duration
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
	companion object {
		const val ACCESS_TOKEN_COOKIE = "registry_token"
		const val REFRESH_TOKEN_COOKIE = "registry_refresh"

		/** The refresh cookie is sent to `/token` and `/token/refresh`, and nowhere else. */
		const val REFRESH_TOKEN_PATH = "/api/v1/authentication/token"

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
		exchange.response.addCookie(
			cookie(REFRESH_TOKEN_COOKIE, token.refreshToken, REFRESH_TOKEN_PATH, STRICT, token.refreshExpiresIn)
		)
	}

	/**
	 * Expires both cookies. A browser only drops a cookie when the replacement matches its name,
	 * domain **and** path, so the attributes here have to mirror [write] exactly.
	 */
	fun clear(exchange: ServerWebExchange) {
		exchange.response.addCookie(cookie(ACCESS_TOKEN_COOKIE, "", ROOT_PATH, sameSite, maxAge = 0))
		exchange.response.addCookie(cookie(REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_PATH, STRICT, maxAge = 0))
	}

	private fun cookie(name: String, value: String, path: String, sameSite: String, maxAge: Long): ResponseCookie {
		val builder = ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite(sameSite)
			.path(path)
			.maxAge(Duration.ofSeconds(maxAge))

		// An empty domain would be written out as `Domain=`, which browsers reject. Leaving the
		// attribute off makes the cookie host-only, which is what local development wants.
		return if (domain.isBlank()) builder.build() else builder.domain(domain).build()
	}
}
