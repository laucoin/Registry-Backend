package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import org.apache.commons.text.similarity.JaroWinklerSimilarity

/**
 * Purpose: Matches a translated label against the text typed by the user in a picker.
 * Scope: Owns the fuzzy comparison (Jaro-Winkler) applied to labels that only exist in the user's language.
 * Limits: Works on presentation labels only; domain search stays in SQL.
 */
object LabelSearchExt {
	private val similarity = JaroWinklerSimilarity()

	fun String.matchesQuery(query: String?): Boolean {
		return query == null || similarity.apply(this, query) > 0
	}
}
