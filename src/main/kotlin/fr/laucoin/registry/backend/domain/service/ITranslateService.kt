package fr.laucoin.registry.backend.domain.service

import java.util.Locale
import org.springframework.context.i18n.LocaleContextHolder

interface ITranslateService {
	// The `locale` default only resolves correctly on the normal request-handling path — Spring's
	// exception-translation and @ExceptionHandler dispatch don't carry the Reactor Context that backs
	// LocaleContextHolder here, so any caller with direct access to the ServerWebExchange (error
	// handlers, entry points) must resolve and pass the locale explicitly instead of relying on this.
	fun getMessage(
		code: String,
		args: Array<Any>? = null,
		default: String? = null,
		locale: Locale = LocaleContextHolder.getLocale(),
	): String

	fun getError(
		code: String,
		args: Array<Any>? = null,
		default: String? = null,
		locale: Locale = LocaleContextHolder.getLocale(),
	): String
}