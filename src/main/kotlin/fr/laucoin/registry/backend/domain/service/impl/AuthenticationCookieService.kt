package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.TokenModel
import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component

@Component
class AuthenticationCookieService(
	@param:Value($$"${registry.server.prefix}")
	private val apiPrefix: String,
	@param:Value($$"${registry.security.cookie.secure}")
	private val secure: Boolean,
) {
	companion object {
		const val ACCESS_TOKEN_COOKIE = "registry_access_token"
		const val REFRESH_TOKEN_COOKIE = "registry_refresh_token"
	}

	/**
	 * Scoped to the whole authentication sub-tree, not just `/token` — a cookie's path only covers
	 * itself and its own sub-paths, so a narrower scope would silently exclude sibling routes like
	 * `/logout/uri`, which needs this cookie to revoke the refresh token at the identity provider.
	 */
	private val refreshTokenPath: String get() = "$apiPrefix/v1/authentication"

	fun setAuthCookies(response: ServerHttpResponse, token: TokenModel) {
		response.addCookie(buildCookie(ACCESS_TOKEN_COOKIE, token.accessToken, apiPrefix, Duration.ofSeconds(token.expiresIn)))
		response.addCookie(buildCookie(REFRESH_TOKEN_COOKIE, token.refreshToken, refreshTokenPath, maxAge = null))
	}

	fun clearAuthCookies(response: ServerHttpResponse) {
		response.addCookie(buildCookie(ACCESS_TOKEN_COOKIE, "", apiPrefix, Duration.ZERO))
		response.addCookie(buildCookie(REFRESH_TOKEN_COOKIE, "", refreshTokenPath, Duration.ZERO))
	}

	fun extractAccessToken(request: ServerHttpRequest): String? = request.cookies.getFirst(ACCESS_TOKEN_COOKIE)?.value

	fun extractRefreshToken(request: ServerHttpRequest): String? = request.cookies.getFirst(REFRESH_TOKEN_COOKIE)?.value

	private fun buildCookie(name: String, value: String, path: String, maxAge: Duration?): ResponseCookie {
		val builder = ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite("Lax")
			.path(path)
		maxAge?.let { builder.maxAge(it) }
		return builder.build()
	}
}
