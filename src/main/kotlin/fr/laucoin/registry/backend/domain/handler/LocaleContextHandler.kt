package fr.laucoin.registry.backend.domain.handler

import org.springframework.context.i18n.LocaleContextThreadLocalAccessor
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import reactor.util.context.Context

/**
 * Resolves the request's locale once and writes it into the Reactor [Context] for the rest of the
 * chain, so [LocaleContextThreadLocalAccessor]-based propagation (wired in [I18nConfig]) makes it
 * available wherever `LocaleContextHolder`/[ITranslateService] read the ambient locale downstream.
 */
@Component
class LocaleContextHandler(private val localeContextResolver: LocaleContextResolver): WebFilter {

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val localeContext = localeContextResolver.resolveLocaleContext(exchange)
		return chain.filter(exchange)
			.contextWrite(Context.of(LocaleContextThreadLocalAccessor.KEY, localeContext))
	}
}
