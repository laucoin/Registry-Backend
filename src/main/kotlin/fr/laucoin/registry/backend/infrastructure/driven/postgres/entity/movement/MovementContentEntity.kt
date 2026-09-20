package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import java.time.LocalDate
import java.util.UUID

data class MovementContentEntity(
	var id: UUID? = null,

	var movementId: UUID? = null,

	var poolName: String? = null,

	var participantId: UUID? = null,
	var participantFirstName: String? = null,
	var participantLastName: String? = null,
	var participantBirthday: LocalDate? = null,
	var participantType: ParticipantTypeEnum? = null,

	var vehicleId: UUID? = null,
	var vehicleLicensePlate: String? = null,
	var vehicleBrand: String? = null,
	var vehicleModel: String? = null,
)
