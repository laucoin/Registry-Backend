package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
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
            .filter { new -> Objects.isNull(content.find { new.participant?.id == it.participant?.id && new.poolName == it.poolName && new.vehicle?.id == it.vehicle?.id }) }
    }

    fun getNewContentParticipantIds(movement: MovementModel): List<UUID> {
        return getNewContent(movement).mapNotNull { it.participant?.id }
    }

    fun getNewContentVehicleIds(movement: MovementModel): List<UUID> {
        return getNewContent(movement).mapNotNull { it.vehicle?.id }
    }

    fun getNewContentDriverIds(movement: MovementModel): List<UUID> {
        return getNewContent(movement).filter { Objects.nonNull(it.vehicle) }.mapNotNull { it.participant?.id }
    }

    fun isGuestsMovement(): Boolean {
        return contentType === ParticipantTypeEnum.GUEST
    }

    fun atLeastOldGuestIfGuestsEntrance(movement: MovementModel): Boolean {
        if (type != MovementTypeEnum.IN || contentType != ParticipantTypeEnum.GUEST) return false
        return content.any { old -> movement.content.any { new -> old.participant?.id == new.participant?.id } }
    }

    fun getOldContentIds(movement: MovementModel): List<UUID> {
        return content
            .filter { old -> Objects.isNull(movement.content.find { old.participant?.id == it.participant?.id && old.poolName == it.poolName && old.vehicle?.id == it.vehicle?.id }) }
            .mapNotNull(MovementContentModel::id)
    }
}
