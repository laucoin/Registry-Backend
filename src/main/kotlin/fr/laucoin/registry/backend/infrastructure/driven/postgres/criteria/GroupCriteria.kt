package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import java.time.ZonedDateTime

/** Filtres propres à Group, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class GroupCriteria(
	val query: String? = null,
	val isPresent: Boolean? = null,
	val at: ZonedDateTime? = null,
	val hasOnSiteMembers: Boolean? = null,
)
