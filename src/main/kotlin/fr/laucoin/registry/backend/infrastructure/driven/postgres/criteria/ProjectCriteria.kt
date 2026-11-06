package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import java.time.ZonedDateTime

/** Filtres propres à Project, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class ProjectCriteria(
	val query: String? = null,
	val at: ZonedDateTime? = null,
	val isFavorite: Boolean? = null,
)
