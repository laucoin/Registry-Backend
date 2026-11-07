package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import java.time.ZonedDateTime

/** Filtres propres à Vehicle, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class VehicleCriteria(
	val query: String? = null,
	val isAvailable: Boolean? = null,
	val isPresent: Boolean? = null,
	val at: ZonedDateTime? = null,
	val brand: String? = null,
	val model: String? = null,
)
