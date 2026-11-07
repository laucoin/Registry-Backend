package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.NestedColumnsMapper
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Compose l'utilisateur d'un profil de projet lu en base.
 * Périmètre : Extrait les colonnes de l'utilisateur puis délègue au mapper des utilisateurs.
 * Limites : Ne copie aucune propriété lui-même ; la copie est faite par MapStruct.
 */
@Component
class ProjectProfileMappingHelper(
	private val columns: NestedColumnsMapper,
	private val userMapper: UserEntityMapper,
) {
	@Named("userOf")
	fun userOf(entity: ProjectProfileEntity): UserModel? {
		return entity.userId?.let { userMapper.toModel(columns.toUserEntity(entity)) }
	}
}
