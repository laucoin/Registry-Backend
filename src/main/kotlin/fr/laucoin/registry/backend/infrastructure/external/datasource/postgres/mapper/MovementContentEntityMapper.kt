package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementContentEntity
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class MovementContentEntityMapper: IGenericEntityMapper<MovementContentModel, MovementContentEntity> {
    override fun toModel(entity: MovementContentEntity): MovementContentModel {
        return MovementContentModel().apply {
            participant = mapParticipantEntity(entity)
            movementId = entity.movementId
        }
    }

    private fun mapParticipantEntity(entity: MovementContentEntity): ParticipantModel? {
        return if (Objects.isNull(entity.participantId)) null
        else ParticipantModel().apply {
            id = entity.participantId
            firstName = entity.participantFirstName
            lastName = entity.participantLastName
            birthday = entity.participantBirthday
        }
    }

    override fun toEntity(model: MovementContentModel): MovementContentEntity {
        return MovementContentEntity().apply {
            participantId = model.participant?.id
            movementId = model.movementId
        }.fillWithModel(model)
    }
}
