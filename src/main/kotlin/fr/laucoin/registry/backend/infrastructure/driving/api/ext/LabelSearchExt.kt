package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import org.apache.commons.text.similarity.JaroWinklerSimilarity

/**
 * Objectif : Compare un libellé traduit au texte saisi par l'utilisateur dans un sélecteur.
 * Périmètre : Porte la comparaison approximative (Jaro-Winkler) appliquée aux libellés qui n'existent que dans la langue de l'utilisateur.
 * Limites : Ne travaille que sur des libellés de présentation ; la recherche métier reste en SQL.
 */
object LabelSearchExt {
	private val similarity = JaroWinklerSimilarity()

	fun String.matchesQuery(query: String?): Boolean {
		return query == null || similarity.apply(this, query) > 0
	}
}
