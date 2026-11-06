package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleEntity
import java.time.Clock
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget
import org.springframework.stereotype.Component

/**
 * Objectif : Calcule le statut de disponibilité des modèles lus en base, après leur mapping MapStruct.
 * Périmètre : Renseigne le statut des activités, groupes, projets, profils et véhicules à partir de leurs dates et de l'horloge.
 * Limites : Lit uniquement l'heure courante ; ne persiste jamais un statut.
 */
@Component
class AvailabilityMappingHelper(private val clock: Clock) {
	@AfterMapping
	fun fillStatus(entity: ActivityEntity, @MappingTarget model: ActivityModel) {
		model.status = model.buildStatus(clock)
	}

	@AfterMapping
	fun fillStatus(entity: GroupEntity, @MappingTarget model: GroupModel) {
		model.status = model.buildStatus(clock)
	}

	@AfterMapping
	fun fillStatus(entity: ProjectEntity, @MappingTarget model: ProjectModel) {
		model.status = model.buildStatus(clock)
	}

	@AfterMapping
	fun fillStatus(entity: ProjectProfileEntity, @MappingTarget model: ProjectProfileModel) {
		model.availabilityStatus = model.buildStatus(clock)
	}

	@AfterMapping
	fun fillStatus(entity: VehicleEntity, @MappingTarget model: VehicleModel) {
		model.status = model.buildStatus(clock, entity.lastMovementType)
	}
}
