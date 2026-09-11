package fr.laucoin.registry.backend.domain.handler

import org.springframework.context.i18n.LocaleContextThreadLocalAccessor
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import reactor.util.context.Context

@Component
class HeadersHandler(private val localeContextResolver: LocaleContextResolver): WebFilter {

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val localeContext = localeContextResolver.resolveLocaleContext(exchange)
		return chain.filter(exchange)
			.contextWrite(Context.of(LocaleContextThreadLocalAccessor.KEY, localeContext))
	}
}
