package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import java.util.Objects

interface IGenericEventEntityMapper<M: GenericModel, E: GenericEventEntity>: IGenericEntityMapper<M, E> {
    fun mapEventEntity(entity: E): EventModel? {
        return if (Objects.isNull(entity.eventId)) null
        else EventModel().apply {
            id = entity.eventId
            name = entity.eventName
            begin = entity.eventStartTime
            end = entity.eventEndTime
            options = entity.eventOptions
        }
    }
}
