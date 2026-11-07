package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import java.time.ZonedDateTime

/** Filtres propres à Movement, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class MovementCriteria(
	val hasActivity: Boolean? = null,
	val type: List<MovementTypeEnum>,
	val from: ZonedDateTime? = null,
	val to: ZonedDateTime? = null,
)
