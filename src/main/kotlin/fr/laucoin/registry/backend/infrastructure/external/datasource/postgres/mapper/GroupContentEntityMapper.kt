package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupContentEntity
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GroupContentEntityMapper: IEntityReaderMapper<ParticipantModel, GroupContentEntity> {
    override fun toModel(entity: GroupContentEntity): ParticipantModel {
        return ParticipantModel().apply {
            id = entity.participantId
            firstName = entity.participantFirstName
            lastName = entity.participantLastName
            birthday = entity.participantBirthday
        }
    }

    fun toEntity(groupId: UUID, model: ParticipantModel): GroupContentEntity {
        return GroupContentEntity().apply {
            this.groupId = groupId
            participantId = model.id
        }
    }
}
