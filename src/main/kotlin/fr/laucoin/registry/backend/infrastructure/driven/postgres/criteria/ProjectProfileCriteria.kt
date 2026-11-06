package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import java.time.ZonedDateTime

/** Filtres propres à ProjectProfile, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class ProjectProfileCriteria(
	val query: String? = null,
	val isAvailable: Boolean? = null,
	val status: List<ProfileStatusEnum>,
	val at: ZonedDateTime? = null,
	val role: String? = null,
)
