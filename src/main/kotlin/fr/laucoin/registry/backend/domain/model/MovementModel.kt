package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID

data class MovementModel(
	var dateTime: ZonedDateTime = ZonedDateTime.now(),
	var type: MovementTypeEnum? = null,
	var reason: MovementReasonEnum? = null,
	var activity: ActivityModel? = null,
	var contentType: ParticipantTypeEnum = ParticipantTypeEnum.REGISTERED,
	var content: List<MovementContentModel> = emptyList(),
): GenericProjectModel() {
	data class MovementContentModel(
		var id: UUID? = null,
		var poolName: String? = null,
		var participant: ParticipantModel? = null,
		var vehicle: VehicleModel? = null,
	)

	fun getNewContent(movement: MovementModel): List<MovementContentModel> {
		return movement.content
			.filter { newContent -> Objects.isNull(content.find { newContent.participant?.id == it.participant?.id && newContent.poolName == it.poolName && newContent.vehicle?.id == it.vehicle?.id }) }
	}

	fun isLastParticipantMovement(): Boolean {
		return reason === DEFINITIVE_DEPARTURE || (type === OUT && isGuestsMovement())
	}

	private fun isGuestsMovement(): Boolean {
		return contentType === ParticipantTypeEnum.GUEST
	}

	fun getOldContentIds(movement: MovementModel): List<UUID> {
		return content
			.filter { oldContent -> Objects.isNull(movement.content.find { oldContent.participant?.id == it.participant?.id && oldContent.poolName == it.poolName && oldContent.vehicle?.id == it.vehicle?.id }) }
			.mapNotNull(MovementContentModel::id)
	}
}
