package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import java.util.UUID

/** Filtres propres à User, liés aux requêtes SQL par `:#{#c.…}` ; la visibilité reste un paramètre commun. */
data class UserCriteria(
	val query: String? = null,
	val role: String? = null,
	val memberOfProjectId: UUID? = null,
)
