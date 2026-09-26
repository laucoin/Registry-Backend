package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

data class CommunicationEntity(
	var dateTime: ZonedDateTime? = null,
	var message: String? = null,

	var movementId: UUID? = null,
	var movementDateTime: ZonedDateTime? = null,
	var movementType: MovementTypeEnum? = null,
	var movementReason: MovementReasonEnum? = null,

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

	var alertId: UUID? = null,
	var alertDateTime: ZonedDateTime? = null,
	var alertTitle: String? = null,
	var alertStatus: AlertStatusEnum? = null,
): GenericProjectEntity()
