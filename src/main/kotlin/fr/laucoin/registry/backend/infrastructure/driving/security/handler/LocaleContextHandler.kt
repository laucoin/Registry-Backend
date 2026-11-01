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
 * Objectif : Propage la locale de la requête vers le contexte réactif.
 * Périmètre : Résout la locale une fois et l'écrit dans le contexte pour que les messages soient localisés malgré les changements de thread.
 * Limites : Ne choisit pas les règles de locale ; le resolver s'en charge.
 */
@Component
class LocaleContextHandler(private val localeContextResolver: LocaleContextResolver): WebFilter {

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val localeContext = localeContextResolver.resolveLocaleContext(exchange)
		return chain.filter(exchange)
			.contextWrite(Context.of(LocaleContextThreadLocalAccessor.KEY, localeContext))
	}
}
