package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.NestedColumnsMapper
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Compose le projet d'un comptage de profils administrateurs lu en base.
 * Périmètre : Extrait l'identifiant et le nom du projet puis délègue au mapper des projets.
 * Limites : Ne copie aucune propriété lui-même ; la copie est faite par MapStruct.
 */
@Component
class RoleCountMappingHelper(
	private val columns: NestedColumnsMapper,
	private val projectMapper: ProjectEntityMapper,
) {
	@Named("projectOf")
	fun projectOf(entity: ProjectProfileRoleCountEntity): ProjectModel? {
		return entity.projectId?.let { projectMapper.toModel(columns.toProjectEntity(entity)) }
	}
}
