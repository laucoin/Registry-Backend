package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import kotlin.time.Duration
import org.springframework.stereotype.Component

@Component
class ActivityEntityMapper: IEntityMapper<ActivityModel, ActivityEntity> {
    override fun toModel(entity: ActivityEntity): ActivityModel {
        return ActivityModel().apply {
            name = entity.name
            description = entity.description
            duration = if (Objects.nonNull(entity.duration)) Duration.parse(entity.duration !!) else null
            allowedParticipants = if (Objects.nonNull(entity.minAllowedParticipants) || Objects.nonNull(entity.maxAllowedParticipants))
                NumericRangeModel(entity.minAllowedParticipants, entity.maxAllowedParticipants)
            else null
            startAvailability = if (Objects.isNull(entity.startAvailabilityDate)) null
            else CustomDateTimeModel(entity.startAvailabilityDate !!, entity.startAvailabilityTime)
            endAvailability = if (Objects.isNull(entity.endAvailabilityDate)) null
            else CustomDateTimeModel(entity.endAvailabilityDate !!, entity.endAvailabilityTime)
        }.fillWithProjectAndEntity(entity)
    }

    override fun toEntity(model: ActivityModel): ActivityEntity {
        return ActivityEntity().apply {
            name = model.name
            description = model.description
            duration = if (Objects.nonNull(model.duration)) model.duration.toString() else null
            minAllowedParticipants = model.allowedParticipants?.lower
            maxAllowedParticipants = model.allowedParticipants?.upper
            startAvailabilityDate = model.startAvailability?.date
            startAvailabilityTime = model.startAvailability?.time
            endAvailabilityDate = model.endAvailability?.date
            endAvailabilityTime = model.endAvailability?.time
        }.fillWithProjectAndModel(model)
    }
}
