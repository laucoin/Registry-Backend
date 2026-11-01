package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ParticipantEntityMapper(
	private val objectMapper: ObjectMapper,
	private val userMapper: UserEntityMapper,
): IEntityMapper<ParticipantModel, ParticipantEntity> {
	override fun toModel(entity: ParticipantEntity): ParticipantModel {
		return ParticipantModel().apply {
			firstName = entity.firstName
			lastName = entity.lastName
			birthday = entity.birthday
			type = entity.type
			groups = extractGroups(entity.groups)
			availableGroups = groups.filter { buildPresentGroupIds(entity.availableGroups).contains(it.id) }
			startAvailability = mapCustomDateTime(entity.startAvailabilityDate, entity.startAvailabilityTime)
			endAvailability = mapCustomDateTime(entity.endAvailabilityDate, entity.endAvailabilityTime)
			status = entity.status
			lastMovement = entity.lastMovementDateTime
			effectiveArrival = entity.effectiveArrival
			effectiveDeparture = entity.effectiveDeparture
			user = mapUser(entity)
		}.fillWithProjectAndEntity(entity)
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

	private fun mapUser(entity: ParticipantEntity): UserModel? {
		return Optional.ofNullable(entity.userId).map {
			userMapper.toModel(UserEntity().apply {
				id = it
				firstName = entity.userFirstName
				lastName = entity.userLastName
				email = entity.userEmail
			})
		}.orElse(null)
	}

	override fun toEntity(model: ParticipantModel): ParticipantEntity {
		return ParticipantEntity().apply {
			firstName = model.firstName
			lastName = model.lastName
			birthday = model.birthday
			type = model.type
			startAvailabilityDate = model.startAvailability?.date
			startAvailabilityTime = model.startAvailability?.time
			endAvailabilityDate = model.endAvailability?.date
			endAvailabilityTime = model.endAvailability?.time
			userId = model.user?.id
		}.fillWithProjectAndModel(model)
	}
}
