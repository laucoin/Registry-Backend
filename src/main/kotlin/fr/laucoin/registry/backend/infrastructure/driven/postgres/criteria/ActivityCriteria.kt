package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import java.time.ZonedDateTime

/** Filtres propres à Activity, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class ActivityCriteria(
	val query: String? = null,
	val isAvailable: Boolean? = null,
	val at: ZonedDateTime? = null,
	val minDuration: String? = null,
	val maxDuration: String? = null,
	val participants: Int? = null,
)
