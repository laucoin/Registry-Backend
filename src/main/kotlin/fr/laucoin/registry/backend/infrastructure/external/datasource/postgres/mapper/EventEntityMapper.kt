package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithModel
import org.springframework.stereotype.Component

@Component
class EventEntityMapper: IEntityMapper<EventModel, EventEntity> {
    override fun toModel(entity: EventEntity): EventModel {
        return EventModel().apply {
            name = entity.name
            begin = entity.begin
            end = entity.end
            options = entity.options
        }.fillWithEntity(entity)
    }

    override fun toEntity(model: EventModel): EventEntity {
        return EventEntity().apply {
            name = model.name
            begin = model.begin
            end = model.end
            options = model.options
        }.fillWithModel(model)
    }
}
