package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementContentEntity
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class MovementContentEntityMapper: IEntityReaderMapper<MovementContentModel, MovementContentEntity> {
    override fun toModel(entity: MovementContentEntity): MovementContentModel {
        return MovementContentModel(
            id = entity.id,
            poolName = entity.poolName,
            participant = mapParticipantEntity(entity),
        )
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

    fun toEntity(movementId: UUID, model: MovementContentModel): MovementContentEntity {
        return MovementContentEntity().apply {
            this.movementId = movementId
            poolName = model.poolName
            participantId = model.participant?.id
        }
    }
}
