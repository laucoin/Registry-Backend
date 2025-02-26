package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithModel
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class EventEntityMapper: IEntityMapper<EventModel, EventEntity> {
    override fun toModel(entity: EventEntity): EventModel {
        return EventModel().apply {
            name = entity.name
            begin = if (Objects.isNull(entity.beginDate)) null
            else CustomDateTimeModel(entity.beginDate !!, entity.beginTime)
            end = if (Objects.isNull(entity.endDate)) null
            else CustomDateTimeModel(entity.endDate !!, entity.endTime)
            options = entity.options
        }.fillWithEntity(entity)
    }

    override fun toEntity(model: EventModel): EventEntity {
        return EventEntity().apply {
            name = model.name
            beginDate = model.begin?.date
            beginTime = model.begin?.time
            endDate = model.end?.date
            endTime = model.end?.time
            options = model.options
        }.fillWithModel(model)
    }
}
