package fr.laucoin.registry.backend.domain.service.support

import java.util.Locale
import org.springframework.context.i18n.LocaleContextHolder

/**
 * Objectif : Contrat de traduction des messages et des erreurs.
 * Périmètre : Déclare la résolution d'une clé i18n dans une locale donnée.
 * Limites : Contrat uniquement ; ne décide pas de la locale, l'appelant la fournit.
 */
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