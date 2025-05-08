package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.time.ZonedDateTime
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class CommunicationEntityMapper(
    private val movementMapper: MovementEntityMapper,
): IEntityMapper<CommunicationModel, CommunicationEntity> {
    override fun toModel(entity: CommunicationEntity): CommunicationModel {
        return CommunicationModel().apply {
            dateTime = entity.dateTime ?: ZonedDateTime.now()
            message = entity.message
            movement = if (Objects.nonNull(entity.movementId)) movementMapper.toModel(MovementEntity().apply {
                id = entity.movementId
                dateTime = entity.movementDateTime
                type = entity.movementType
                reason = entity.movementReason
                activityId = entity.activityId
                activityName = entity.activityName
                activityDescription = entity.activityDescription
                activityDuration = entity.activityDuration
                activityMinAllowedParticipants = entity.activityMinAllowedParticipants
                activityMaxAllowedParticipants = entity.activityMaxAllowedParticipants
                activityStartAvailabilityDate = entity.activityStartAvailabilityDate
                activityStartAvailabilityTime = entity.activityStartAvailabilityTime
                activityEndAvailabilityDate = entity.activityEndAvailabilityDate
                activityEndAvailabilityTime = entity.activityEndAvailabilityTime
            }) else null
        }.fillWithProjectAndEntity(entity)
    }

    override fun toEntity(model: CommunicationModel): CommunicationEntity {
        return CommunicationEntity().apply {
            dateTime = model.dateTime
            message = model.message
            movementId = model.movement?.id
        }.fillWithProjectAndModel(model)
    }
}
