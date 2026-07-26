package fr.laucoin.registry.backend.domain.handler

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono

@Component
class HeadersHandler(private val localeContextResolver: LocaleContextResolver) : WebFilter {

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val localeContext = localeContextResolver.resolveLocaleContext(exchange)
		return Mono.deferContextual {
			val locale = localeContext.locale
			val previous = LocaleContextHolder.getLocale()
			LocaleContextHolder.setLocale(locale)
			chain.filter(exchange)
				.doFinally {
					LocaleContextHolder.setLocale(previous)
				}
		}
	}
}
