package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isBeforeOrEqual
import fr.laucoin.registry.backend.domain.extension.DateExt.isEqualOrAfter
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.util.Objects

object AvailabilityElementExt {
    fun ParticipantModel.buildStatus(lastMovementType: MovementTypeEnum?): PresenceStatusEnum {
        val now = CustomDateTimeModel.now()
        val available = (
                (
                        Objects.isNull(startAvailability?.date) && (groups.isEmpty() || availableGroups.isNotEmpty())
                        || startAvailability.isBeforeOrEqual(now)
                ) &&
                (
                        Objects.isNull(endAvailability?.date) && (groups.isEmpty() || availableGroups.isNotEmpty())
                        || endAvailability.isEqualOrAfter(now)
                )
                        )
        return status(available, lastMovementType)
    }

    fun VehicleModel.buildStatus(lastMovementType: MovementTypeEnum?): PresenceStatusEnum {
        val now = CustomDateTimeModel.now()
        val available = startAvailability.isBeforeOrEqual(now) && endAvailability.isEqualOrAfter(now)
        return status(available, lastMovementType)
    }

    private fun status(available: Boolean, lastMovementType: MovementTypeEnum?): PresenceStatusEnum {
        return when {
            available && lastMovementType === MovementTypeEnum.IN -> PresenceStatusEnum.IN
            ! available -> PresenceStatusEnum.UNAVAILABLE
            else -> PresenceStatusEnum.OUT
        }
    }

    fun ProjectModel.buildStatus(): AvailabilityStatusEnum {
        val now = CustomDateTimeModel.now()
        val available = begin.isBeforeOrEqual(now) && end.isEqualOrAfter(now)
        return availability(available)
    }

    fun ProjectProfileModel.buildStatus(): AvailabilityStatusEnum {
        val now = CustomDateTimeModel.now()
        val available = startAccess.isBeforeOrEqual(now) && endAccess.isEqualOrAfter(now)
        return availability(available)
    }

    fun GroupModel.buildStatus(): AvailabilityStatusEnum {
        val now = CustomDateTimeModel.now()
        val available = startAvailability.isBeforeOrEqual(now) && endAvailability.isEqualOrAfter(now)
        return availability(available)
    }

    fun ActivityModel.buildStatus(): AvailabilityStatusEnum {
        val now = CustomDateTimeModel.now()
        val available = startAvailability.isBeforeOrEqual(now) && endAvailability.isEqualOrAfter(now)
        return availability(available)
    }

    private fun availability(available: Boolean): AvailabilityStatusEnum {
        return if (available) AvailabilityStatusEnum.AVAILABLE else AvailabilityStatusEnum.UNAVAILABLE
    }
}
