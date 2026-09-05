package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.ACCESS_TOKEN_COOKIE
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Reads the bearer token from the `Authorization` header, falling back to the session cookie.
 *
 * Keeping the header path is what lets Swagger UI, server-to-server callers and any future
 * non-browser client keep working, and it makes the migration to cookies safe to deploy in stages:
 * a backend released before the frontend still accepts the header traffic the old frontend sends.
 *
 * **The header deliberately wins over the cookie.** An `Authorization` header is something a caller
 * sets on purpose; a cookie is attached by the browser whether the caller meant it or not. Beyond
 * the obvious — Swagger's *Authorize* would otherwise be silently overridden by whatever session
 * the developer happens to have open in the same browser — this ordering is what keeps the CSRF
 * exemption honest. That exemption skips CSRF for requests carrying an `Authorization` header, on
 * the grounds that such a caller cannot be a CSRF victim. Were the cookie to win, a request could
 * carry a meaningless header to buy the exemption while still authenticating through the ambient
 * cookie. With the header first, a bogus header authenticates as nothing and the request is
 * rejected, so the exemption can never be claimed without also being the credential in use.
 */
@Component
class TokenExtractionHandler: ServerAuthenticationConverter {
	private val headerConverter = ServerBearerTokenAuthenticationConverter()

	override fun convert(exchange: ServerWebExchange): Mono<Authentication> =
		headerConverter.convert(exchange).switchIfEmpty { fromCookie(exchange) }

	private fun fromCookie(exchange: ServerWebExchange): Mono<Authentication> {
		val token = exchange.request.cookies.getFirst(ACCESS_TOKEN_COOKIE)?.value
		return if (token.isNullOrBlank()) Mono.empty()
		else Mono.just(BearerTokenAuthenticationToken(token))
	}
}
