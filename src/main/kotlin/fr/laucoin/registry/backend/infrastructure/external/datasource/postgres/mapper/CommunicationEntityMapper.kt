package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.time.ZonedDateTime
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class CommunicationEntityMapper(
    private val movementMapper: MovementEntityMapper,
    private val alertMapper: AlertEntityMapper,
): IEntityMapper<CommunicationModel, CommunicationEntity> {
    override fun toModel(entity: CommunicationEntity): CommunicationModel {
        return CommunicationModel().apply {
            dateTime = entity.dateTime ?: ZonedDateTime.now()
            message = entity.message
            movement = mapMovement(entity)
            alert = mapAlert(entity)
        }.fillWithProjectAndEntity(entity)
    }

    private fun mapMovement(entity: CommunicationEntity): MovementModel? {
        return Optional.ofNullable(entity.movementId).map {
            movementMapper.toModel(MovementEntity().apply {
                id = it
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
            })
        }.orElse(null)
    }

    private fun mapAlert(entity: CommunicationEntity): AlertModel? {
        return Optional.ofNullable(entity.alertId).map {
            alertMapper.toModel(AlertEntity().apply {
                id = it
                dateTime = entity.alertDateTime
                title = entity.alertTitle
                status = entity.alertStatus
            })
        }.orElse(null)
    }

    override fun toEntity(model: CommunicationModel): CommunicationEntity {
        return CommunicationEntity().apply {
            dateTime = model.dateTime
            message = model.message
            movementId = model.movement?.id
            alertId = model.alert?.id
        }.fillWithProjectAndModel(model)
    }
}
