package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.util.Objects

/**
 * Objectif : Calcule le statut de disponibilité des éléments dotés d'une fenêtre de disponibilité.
 * Périmètre : Construit le statut des projets, groupes, activités, véhicules et profils à partir de leurs dates.
 * Limites : Lit uniquement l'heure courante ; ne persiste jamais un statut.
 */
object AvailabilityElementExt {
	fun VehicleModel.buildStatus(lastMovementType: MovementTypeEnum?): PresenceStatusEnum {
		val now = CustomDateTimeModel.now()
		val available = now.asStartIsAfterOther(startAvailability) && now.asEndIsBeforeOther(endAvailability)
		return status(available, lastMovementType)
	}

	private fun status(available: Boolean, lastMovementType: MovementTypeEnum?): PresenceStatusEnum {
		return when {
			available && lastMovementType === MovementTypeEnum.IN -> PresenceStatusEnum.IN
			!available -> PresenceStatusEnum.UNAVAILABLE
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
