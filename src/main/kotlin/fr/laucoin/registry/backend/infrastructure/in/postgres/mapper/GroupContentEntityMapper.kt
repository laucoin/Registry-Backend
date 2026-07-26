package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupContentEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GroupContentEntityMapper : IEntityReaderMapper<ParticipantModel, GroupContentEntity> {
	override fun toModel(entity: GroupContentEntity): ParticipantModel {
		return ParticipantModel().apply {
			id = entity.participantId
			firstName = entity.participantFirstName
			lastName = entity.participantLastName
			birthday = entity.participantBirthday
			type = entity.participantType
		}
	}

	fun toEntity(groupId: UUID, model: ParticipantModel): GroupContentEntity {
		return GroupContentEntity().apply {
			this.groupId = groupId
			participantId = model.id
		}
	}
}
