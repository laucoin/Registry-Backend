package fr.laucoin.registry.backend.domain.service

import java.util.Locale
import org.springframework.context.i18n.LocaleContextHolder

interface ITranslateService {
	// Le défaut de `locale` ne se résout correctement que sur le chemin normal de traitement des requêtes — la traduction
	// d'exceptions de Spring et le dispatch @ExceptionHandler ne transportent pas le Reactor Context qui alimente
	// LocaleContextHolder ici ; tout appelant ayant un accès direct au ServerWebExchange (gestionnaires
	// d'erreurs, entry points) doit donc résoudre et passer la locale explicitement au lieu de s'appuyer dessus.
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