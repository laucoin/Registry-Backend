package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum

data class PlannedMovementModel(
	val type: MovementTypeEnum,
	val plannedAt: CustomDateTimeModel?,
	val participants: List<ParticipantModel>,
	val poolName: String? = null,
) {
	val reason: MovementReasonEnum? = if (type == IN) null else MovementReasonEnum.DEFINITIVE_DEPARTURE
	val contentType: ParticipantTypeEnum = ParticipantTypeEnum.REGISTERED

	init {
		require(participants.isNotEmpty()) { "A planned movement needs at least one participant" }
		require(poolName != null || participants.size == 1) { "Several participants must share a pool name" }
	}
}
