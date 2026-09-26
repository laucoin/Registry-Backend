package fr.laucoin.registry.backend.domain.service

import org.springframework.context.i18n.LocaleContextHolder
import java.util.Locale

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