package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import java.time.LocalDate
import java.time.ZonedDateTime

/** Filtres propres à Participant, liés aux requêtes SQL par `:#{#c.…}` ; le projet et la visibilité restent des paramètres communs. */
data class ParticipantCriteria(
	val query: String? = null,
	val isMajor: Boolean? = null,
	val type: ParticipantTypeEnum? = null,
	val isAvailable: Boolean? = null,
	val status: PresenceStatusEnum? = null,
	val presence: PresenceStatusEnum? = null,
	val at: ZonedDateTime? = null,
	val hasUser: Boolean? = null,
	val isOnSite: Boolean? = null,
	val birthdayFrom: LocalDate? = null,
	val birthdayTo: LocalDate? = null,
)
