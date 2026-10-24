package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_NOT_SUPPORTED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import org.springframework.http.HttpStatus.BAD_REQUEST

/**
 * Objectif : Transforme la valeur brute `sort` de la requête en instruction de tri validée.
 * Périmètre : Porte la syntaxe `field` / `-field` et la vérification de la liste autorisée par ressource.
 * Limites : Gère une seule clé de tri et ne construit jamais de SQL ; un champ non supporté est renvoyé tronqué et débarrassé de ses caractères de contrôle.
 */
object SortExt {
	private const val DESCENDING_PREFIX = "-"
	private const val MAX_ECHOED_LENGTH = 32

	fun String?.toSortModel(allowedFields: Set<String>): SortModel? {
		val raw = this?.trim()?.takeUnless { it.isEmpty() } ?: return null
		val isDescending = raw.startsWith(DESCENDING_PREFIX)
		val field = raw.removePrefix(DESCENDING_PREFIX)

		if (field !in allowedFields) {
			throw RegistryException(BAD_REQUEST, SORT_FIELD_NOT_SUPPORTED, arrayListOf(field.echoable(), allowedFields.sorted().joinToString(", ")))
		}
		return SortModel(field, isDescending)
	}

	private fun String.echoable(): String = take(MAX_ECHOED_LENGTH).filter { it.isLetterOrDigit() || it == '_' }
}
