package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.util.Objects

object AvailabilityElementExt {
	fun ParticipantModel.isAvailableNow(): Boolean {
		val now = CustomDateTimeModel.now()
		return (
				(
						Objects.isNull(startAvailability) && (groups.isEmpty() || availableGroups.isNotEmpty())
								|| now.asStartIsAfterOther(startAvailability)
						) &&
						(
								Objects.isNull(endAvailability) && (groups.isEmpty() || availableGroups.isNotEmpty())
										|| now.asEndIsBeforeOther(endAvailability)
								)
				)
	}

	fun ParticipantModel.buildStatus(lastMovementType: MovementTypeEnum?): PresenceStatusEnum {
		if (Objects.nonNull(departedAt)) return PresenceStatusEnum.DEPARTED
		return status(isAvailableNow(), lastMovementType)
	}

	fun ParticipantModel.buildAvailabilityWarning(lastMovementType: MovementTypeEnum?): Boolean {
		return Objects.isNull(departedAt) && Objects.nonNull(lastMovementType) && !isAvailableNow()
	}

	fun VehicleModel.isAvailableNow(): Boolean {
		val now = CustomDateTimeModel.now()
		return now.asStartIsAfterOther(startAvailability) && now.asEndIsBeforeOther(endAvailability)
	}

	fun VehicleModel.buildStatus(lastMovementType: MovementTypeEnum?): PresenceStatusEnum {
		return status(isAvailableNow(), lastMovementType)
	}

	fun VehicleModel.buildAvailabilityWarning(lastMovementType: MovementTypeEnum?): Boolean {
		return Objects.nonNull(lastMovementType) && !isAvailableNow()
	}

	/**
	 * A recorded movement is a fact, an availability window is a plan: the window
	 * therefore never hides a movement any more, it only decorates it with a
	 * warning (`buildAvailabilityWarning`). `UNAVAILABLE` keeps the one case no
	 * movement contradicts — nobody ever moved this person, and the window does not
	 * contain now — so that everyone who HAS moved lands in `IN` or `OUT` and no
	 * head count can lose them. Departure is answered before this, from the
	 * register rather than from the window.
	 */
	private fun status(available: Boolean, lastMovementType: MovementTypeEnum?): PresenceStatusEnum {
		return when {
			lastMovementType === MovementTypeEnum.IN -> PresenceStatusEnum.IN
			Objects.isNull(lastMovementType) && !available -> PresenceStatusEnum.UNAVAILABLE
			else -> PresenceStatusEnum.OUT
		}
	}

	fun ProjectModel.buildStatus(): AvailabilityStatusEnum {
		val now = CustomDateTimeModel.now()
		val available = now.asStartIsAfterOther(begin) && now.asEndIsBeforeOther(end)
		return availability(available)
	}

	fun ProjectProfileModel.buildStatus(): AvailabilityStatusEnum {
		val now = CustomDateTimeModel.now()
		val available = now.asStartIsAfterOther(startAccess) && now.asEndIsBeforeOther(endAccess)
		return availability(available)
	}

	fun GroupModel.buildStatus(): AvailabilityStatusEnum {
		val now = CustomDateTimeModel.now()
		val available = now.asStartIsAfterOther(startAvailability) && now.asEndIsBeforeOther(endAvailability)
		return availability(available)
	}

	fun ActivityModel.buildStatus(): AvailabilityStatusEnum {
		val now = CustomDateTimeModel.now()
		val available = now.asStartIsAfterOther(startAvailability) && now.asEndIsBeforeOther(endAvailability)
		return availability(available)
	}

	private fun availability(available: Boolean): AvailabilityStatusEnum {
		return if (available) AvailabilityStatusEnum.AVAILABLE else AvailabilityStatusEnum.UNAVAILABLE
	}
}
