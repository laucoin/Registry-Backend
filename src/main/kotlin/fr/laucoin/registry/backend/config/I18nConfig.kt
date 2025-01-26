package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_IMPLEMENTED_YET
import fr.laucoin.registry.backend.domain.model.RegistryException
import java.util.Locale
import kotlin.text.Charsets.UTF_8
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.i18n.LocaleContext
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.i18n.LocaleContextResolver

@Configuration
class I18nConfig(
    @Value("\${registry.information.locale.default}")
    val defaultLocale: Locale,
): LocaleContextResolver {
    companion object {
        private const val I18N_MESSAGE: String = "classpath:i18n/messages"
        private const val I18N_ERROR: String = "classpath:i18n/errors"
    }

    @Bean(name = ["messagesSource"])
    fun messagesSource(): MessageSource {
        Locale.setDefault(defaultLocale)
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename(I18N_MESSAGE)
        messageSource.setDefaultEncoding(UTF_8.name())
        messageSource.setUseCodeAsDefaultMessage(true)
        messageSource.setDefaultLocale(defaultLocale)
        return messageSource
    }

    @Bean(name = ["errorsSource"])
    fun errorsSource(): MessageSource {
        Locale.setDefault(defaultLocale)
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename(I18N_ERROR)
        messageSource.setDefaultEncoding(UTF_8.name())
        messageSource.setUseCodeAsDefaultMessage(true)
        messageSource.setDefaultLocale(defaultLocale)
        return messageSource
    }

    override fun resolveLocaleContext(exchange: ServerWebExchange): LocaleContext {
        val language: String? = exchange.request.headers.getFirst(ACCEPT_LANGUAGE)
        return SimpleLocaleContext(Locale.forLanguageTag(language) ?: Locale.getDefault())
    }

    override fun setLocaleContext(exchange: ServerWebExchange, localeContext: LocaleContext?) {
        throw RegistryException(NOT_IMPLEMENTED, NOT_IMPLEMENTED_YET)
    }
}
