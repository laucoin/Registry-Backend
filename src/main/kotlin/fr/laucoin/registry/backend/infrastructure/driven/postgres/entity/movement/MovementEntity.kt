package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

data class MovementEntity(
	var dateTime: ZonedDateTime? = null,
	var type: MovementTypeEnum? = null,

	var reason: MovementReasonEnum? = null,

	var activityId: UUID? = null,
	var activityName: String? = null,
	var activityDescription: String? = null,
	var activityDuration: String? = null,
	var activityMinAllowedParticipants: Int? = null,
	var activityMaxAllowedParticipants: Int? = null,
	var activityStartAvailabilityDate: LocalDate? = null,
	var activityStartAvailabilityTime: OffsetTime? = null,
	var activityEndAvailabilityDate: LocalDate? = null,
	var activityEndAvailabilityTime: OffsetTime? = null,
): GenericProjectEntity()
