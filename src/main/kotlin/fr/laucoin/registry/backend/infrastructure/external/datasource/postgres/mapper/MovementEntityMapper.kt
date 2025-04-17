package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndModel
import java.time.ZonedDateTime
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class MovementEntityMapper(
    private val activityMapper: ActivityEntityMapper
): IEntityMapper<MovementModel, MovementEntity> {
    override fun toModel(entity: MovementEntity): MovementModel {
        return MovementModel().apply {
            dateTime = entity.dateTime ?: ZonedDateTime.now()
            type = entity.type
            reason = entity.reason
            activity = if (Objects.nonNull(entity.activityId)) activityMapper.toModel(ActivityEntity().apply {
                id = entity.activityId
                name = entity.activityName
                description = entity.activityDescription
                duration = entity.activityDuration
                minAllowedParticipants = entity.activityMinAllowedParticipants
                maxAllowedParticipants = entity.activityMaxAllowedParticipants
                startAvailabilityDate = entity.activityStartAvailabilityDate
                startAvailabilityTime = entity.activityStartAvailabilityTime
                endAvailabilityDate = entity.activityEndAvailabilityDate
                endAvailabilityTime = entity.activityEndAvailabilityTime
            }) else null
        }.fillWithEventAndEntity(entity)
    }

    override fun toEntity(model: MovementModel): MovementEntity {
        return MovementEntity().apply {
            dateTime = model.dateTime
            type = model.type
            reason = model.reason
            activityId = model.activity?.id
        }.fillWithEventAndModel(model)
    }
}
