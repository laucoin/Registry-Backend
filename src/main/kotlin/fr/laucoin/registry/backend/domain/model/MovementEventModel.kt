package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import java.time.ZonedDateTime

data class MovementEventModel(
	val dateTime: ZonedDateTime,
	val type: MovementTypeEnum,
	val reason: MovementReasonEnum? = null,
)
