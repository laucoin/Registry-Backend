package fr.laucoin.registry.backend.domain.service

import org.springframework.context.i18n.LocaleContextHolder
import java.util.Locale

/**
 * Resolves i18n message/error keys against the `messages`/`errors` resource bundles for a given (or
 * ambient) [Locale], falling back to the provided default when a key is missing. Every user-facing
 * string and error message goes through this contract rather than being hardcoded.
 */
interface ITranslateService {
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