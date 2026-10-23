package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import org.springframework.context.i18n.LocaleContextThreadLocalAccessor
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import reactor.util.context.Context

/**
 * Purpose: Propagates the request locale to the reactive context.
 * Scope: Resolves the locale once and writes it to the context so messages are localized across thread hops.
 * Limits: Does not choose the locale rules; the resolver does.
 */
@Component
class LocaleContextHandler(private val localeContextResolver: LocaleContextResolver): WebFilter {

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val localeContext = localeContextResolver.resolveLocaleContext(exchange)
		return chain.filter(exchange)
			.contextWrite(Context.of(LocaleContextThreadLocalAccessor.KEY, localeContext))
	}
}
