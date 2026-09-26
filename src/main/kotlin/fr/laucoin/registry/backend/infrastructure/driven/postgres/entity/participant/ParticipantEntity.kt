package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

data class ParticipantEntity(
	var firstName: String? = null,
	var lastName: String? = null,
	var birthday: LocalDate? = null,
	var type: ParticipantTypeEnum? = null,
	var groups: String? = "[]",
	var availableGroups: String? = "[]",
	var lastMovementType: MovementTypeEnum? = null,
	var lastMovementDateTime: ZonedDateTime? = null,
	var startAvailabilityDate: LocalDate? = null,
	var startAvailabilityTime: OffsetTime? = null,
	var endAvailabilityDate: LocalDate? = null,
	var endAvailabilityTime: OffsetTime? = null,
	var userId: UUID? = null,
	var userFirstName: String? = null,
	var userLastName: String? = null,
	var userEmail: String? = null,
	var purged: Boolean? = null,
): GenericProjectEntity()
