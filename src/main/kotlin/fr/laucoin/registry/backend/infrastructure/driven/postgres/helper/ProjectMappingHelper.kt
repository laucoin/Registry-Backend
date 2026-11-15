package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.ProjectCountsModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget
import org.springframework.stereotype.Component

/**
 * Objectif : Renseigne les compteurs d'un projet lu en base, après son mapping MapStruct.
 * Périmètre : Regroupe les colonnes de comptage et n'expose les compteurs d'une option que si elle est activée.
 * Limites : Ne calcule aucun compteur ; ils proviennent de la requête.
 */
@Component
class ProjectMappingHelper {
	@AfterMapping
	fun fillCounts(entity: ProjectEntity, @MappingTarget model: ProjectModel) {
		val participants = entity.participants ?: return

		model.counts = ProjectCountsModel(
			participants = participants,
			groups = entity.groups ?: 0,
			acceptedProfiles = entity.acceptedProfiles ?: 0,
			vehicles = entity.countIfEnabled(ProjectOptionEnum.VEHICLE, entity.vehicles),
			activities = entity.countIfEnabled(ProjectOptionEnum.ACTIVITY, entity.activities),
			activeAlerts = entity.countIfEnabled(ProjectOptionEnum.ALERT, entity.activeAlerts),
		)
	}

	private fun ProjectEntity.countIfEnabled(option: ProjectOptionEnum, count: Long?): Long? {
		return count.takeIf { option in options.orEmpty() }
	}
}
