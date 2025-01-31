package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ACCEPTED_LANGUAGE_HEADER
import fr.laucoin.registry.backend.domain.model.RegistryException
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class HeadersHandler(
    @Value("\${registry.information.locale.supported}")
    private val supportedLocales: List<String>,
): WebFilter {
    companion object {
        fun headers(request: ServerHttpRequest): Map<String, String> = request.headers.toSingleValueMap()

        fun getLocaleOrThrow(headers: Map<String, String>, supportedLocales: List<String>): Locale {
            val requestedLanguages: List<String> = headers[ACCEPT_LANGUAGE]?.split(",").orEmpty()

            var locale: Locale? = null
            requestedLanguages.forEach {
                val language = supportedLocales.firstOrNull { s -> s.startsWith(it) }
                if (Objects.nonNull(language)) {
                    locale = Locale.forLanguageTag(language)
                    return@forEach
                }
            }

            if (requestedLanguages.isNotEmpty() && Objects.isNull(locale)) {
                throw RegistryException(BAD_REQUEST, NOT_ACCEPTED_LANGUAGE_HEADER)
            }

            return locale ?: Locale.getDefault()
        }
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val headers: Map<String, String> = headers(exchange.request)
        val locale: Locale = getLocaleOrThrow(headers, supportedLocales)

        val mutatedExchange = exchange.mutate()
            .request { builder -> builder.header(ACCEPT_LANGUAGE, locale.language) }
            .build()

        return chain.filter(mutatedExchange).contextWrite { it.put(Locale::class.java, locale) }
    }
}
