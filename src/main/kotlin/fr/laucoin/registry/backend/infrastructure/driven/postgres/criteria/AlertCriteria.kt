package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import java.time.ZonedDateTime

/** Filtres propres à Alert, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class AlertCriteria(
	val query: String? = null,
	val status: List<AlertStatusEnum>? = null,
	val criticality: AlertCriticalityEnum? = null,
	val reinforcedAfterHours: Long = 0L,
	val highAfterHours: Long = 0L,
	val from: ZonedDateTime? = null,
	val to: ZonedDateTime? = null,
)
