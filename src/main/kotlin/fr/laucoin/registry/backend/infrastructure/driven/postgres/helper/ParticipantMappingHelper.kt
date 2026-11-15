package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.NestedColumnsMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.UserEntityMapper
import java.util.UUID
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget
import org.mapstruct.Named
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Objectif : Renseigne l'utilisateur et les groupes d'un participant lu en base, après son mapping MapStruct.
 * Périmètre : Lit les groupes encodés en JSON, en déduit les groupes disponibles et délègue l'utilisateur au mapper des utilisateurs.
 * Limites : Ne modifie aucune donnée ; un JSON de groupes invalide n'est pas toléré.
 */
@Component
class ParticipantMappingHelper(
	private val columns: NestedColumnsMapper,
	private val userMapper: UserEntityMapper,
	private val objectMapper: ObjectMapper,
) {
	@Named("userOf")
	fun userOf(entity: ParticipantEntity): UserModel? {
		return entity.userId?.let { userMapper.toModel(columns.toUserEntity(entity)) }
	}

	@AfterMapping
	fun fillGroups(entity: ParticipantEntity, @MappingTarget model: ParticipantModel) {
		model.groups = extractGroups(entity.groups)
		val presentGroupIds = buildPresentGroupIds(entity.availableGroups)
		model.availableGroups = model.groups.filter { presentGroupIds.contains(it.id) }
	}

	private fun extractGroups(groups: String?): List<GroupModel> {
		if (groups.isNullOrBlank()) return emptyList()
		return objectMapper.readTree(groups)
			.mapNotNull { node -> node.path("id").asString(null)?.let { toGroup(UUID.fromString(it), node.path("name").asString(null)) } }
	}

	private fun toGroup(id: UUID, name: String?): GroupModel = GroupModel(name = name).apply { this.id = id }

	private fun buildPresentGroupIds(availableGroups: String?): List<UUID> {
		if (availableGroups.isNullOrBlank()) return emptyList()
		return objectMapper.readTree(availableGroups).mapNotNull { it.asString(null)?.let(UUID::fromString) }
	}
}
