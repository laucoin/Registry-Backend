package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isBeforeOrEqual
import fr.laucoin.registry.backend.domain.extension.DateExt.isEqualOrAfter
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.util.Objects

object AvailabilityElementExt {
    fun ParticipantModel.buildStatus(lastMovementType: MovementTypeEnum?): UsableElementStatusEnum {
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

    fun VehicleModel.buildStatus(lastMovementType: MovementTypeEnum?): UsableElementStatusEnum {
        val now = CustomDateTimeModel.now()
        val available = startAvailability.isBeforeOrEqual(now) && endAvailability.isEqualOrAfter(now)
        return status(available, lastMovementType)
    }

    private fun status(available: Boolean, lastMovementType: MovementTypeEnum?): UsableElementStatusEnum {
        return when {
            available && lastMovementType === MovementTypeEnum.IN -> UsableElementStatusEnum.IN
            ! available -> UsableElementStatusEnum.UNAVAILABLE
            else -> UsableElementStatusEnum.OUT
        }
    }
}
