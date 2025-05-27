package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.time.ZonedDateTime
import java.util.Objects
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class MovementEntityMapper(
    private val activityMapper: ActivityEntityMapper
): IEntityMapper<MovementModel, MovementEntity> {
    override fun toModel(entity: MovementEntity): MovementModel {
        return MovementModel(contentType = determineContentType(entity)).apply {
            dateTime = entity.dateTime ?: ZonedDateTime.now()
            type = entity.type
            reason = entity.reason
            activity = mapActivity(entity)
            lastCommunicationDateTime = entity.lastCommunicationDateTime
            communicationsCount = entity.communicationsCount
        }.fillWithProjectAndEntity(entity)
    }

    private fun mapActivity(entity: MovementEntity): ActivityModel? {
        return Optional.ofNullable(entity.activityId).map {
            activityMapper.toModel(ActivityEntity().apply {
                id = it
                name = entity.activityName
                description = entity.activityDescription
                duration = entity.activityDuration
                minAllowedParticipants = entity.activityMinAllowedParticipants
                maxAllowedParticipants = entity.activityMaxAllowedParticipants
                startAvailabilityDate = entity.activityStartAvailabilityDate
                startAvailabilityTime = entity.activityStartAvailabilityTime
                endAvailabilityDate = entity.activityEndAvailabilityDate
                endAvailabilityTime = entity.activityEndAvailabilityTime
            })
        }.orElse(null)
    }

    private fun determineContentType(entity: MovementEntity): ParticipantTypeEnum {
        return entity.reason?.participantType ?: if (entity.type == IN || Objects.nonNull(entity.activityId)) REGISTERED else GUEST
    }

    override fun toEntity(model: MovementModel): MovementEntity {
        return MovementEntity().apply {
            dateTime = model.dateTime
            type = model.type
            reason = model.reason
            activityId = model.activity?.id
        }.fillWithProjectAndModel(model)
    }
}
