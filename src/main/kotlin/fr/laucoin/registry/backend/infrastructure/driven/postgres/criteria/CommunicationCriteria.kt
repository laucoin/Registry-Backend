package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import java.time.ZonedDateTime

/** Filtres propres à Communication, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class CommunicationCriteria(
	val query: String? = null,
	val from: ZonedDateTime? = null,
	val to: ZonedDateTime? = null,
)
