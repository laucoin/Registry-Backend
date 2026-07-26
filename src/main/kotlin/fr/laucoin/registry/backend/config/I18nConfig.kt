package fr.laucoin.registry.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver
import java.util.Locale

@Configuration
class I18nConfig(
	@param:Value($$"${registry.information.locale.default}") val defaultLocale: Locale,
	@param:Value($$"${registry.information.locale.supported}") val supportedLocales: List<Locale>
) {

	@Bean(name = ["messagesSource"])
	fun messagesSource(): MessageSource {
		Locale.setDefault(defaultLocale)
		val messageSource = ReloadableResourceBundleMessageSource()
		messageSource.setBasename("classpath:i18n/messages")
		messageSource.setDefaultEncoding(Charsets.UTF_8.name())
		messageSource.setDefaultLocale(defaultLocale)
		return messageSource
	}

	@Bean(name = ["errorsSource"])
	fun errorsSource(): MessageSource {
		Locale.setDefault(defaultLocale)
		val messageSource = ReloadableResourceBundleMessageSource()
		messageSource.setBasename("classpath:i18n/errors")
		messageSource.setDefaultEncoding(Charsets.UTF_8.name())
		messageSource.setDefaultLocale(defaultLocale)
		return messageSource
	}

	@Bean
	fun localeContextResolver(): AcceptHeaderLocaleContextResolver {
		val resolver = AcceptHeaderLocaleContextResolver()
		resolver.setSupportedLocales(supportedLocales)
		resolver.defaultLocale = Locale.getDefault()
		return resolver
	}
}