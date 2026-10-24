package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_NOT_SUPPORTED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import org.springframework.http.HttpStatus.BAD_REQUEST

/**
 * Purpose: Turns the raw `sort` request value into a validated sort instruction.
 * Scope: Owns the `field` / `-field` syntax and the per-resource allow-list check.
 * Limits: Handles a single sort key and never builds SQL; an unsupported field is echoed truncated and stripped of control characters.
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
