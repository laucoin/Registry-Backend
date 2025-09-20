package fr.laucoin.registry.backend.domain.handler

import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class HeadersHandler(
	@param:Value("\${registry.information.locale.supported}")
	private val supportedLocales: List<String>,
): WebFilter {
	companion object {
		fun headers(request: ServerHttpRequest): Map<String, String> = request.headers.toSingleValueMap()

		fun extractLocaleOrDefault(headers: Map<String, String>, supportedLocales: List<String>): Locale {
			val requestedLanguages: List<String> = headers[ACCEPT_LANGUAGE]?.split(",").orEmpty()

			var locale: Locale? = null
			requestedLanguages.forEach {
				val language = supportedLocales.firstOrNull { s -> s.startsWith(it) }
				if (Objects.nonNull(language)) {
					locale = Locale.forLanguageTag(language)
					return@forEach
				}
			}

			return locale ?: Locale.getDefault()
		}
	}

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val headers: Map<String, String> = headers(exchange.request)
		val locale: Locale = extractLocaleOrDefault(headers, supportedLocales)

		val mutatedExchange = exchange.mutate()
			.request { builder -> builder.header(ACCEPT_LANGUAGE, locale.language) }
			.build()

		return chain.filter(mutatedExchange).contextWrite { it.put(Locale::class.java, locale) }
	}
}
