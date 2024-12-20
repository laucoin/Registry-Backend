package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileRoleCountModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileRoleCountEntity
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class EventProfileRoleCountEntityMapper: IEntityMapper<EventProfileRoleCountModel, EventProfileRoleCountEntity> {
    override fun toModel(entity: EventProfileRoleCountEntity): EventProfileRoleCountModel {
        return EventProfileRoleCountModel().apply {
            event = mapEventEntity(entity)
            level0 = entity.level0
        }
    }

    fun mapEventEntity(entity: EventProfileRoleCountEntity): EventModel? {
        return if (Objects.isNull(entity.eventId)) null
        else EventModel().apply {
            id = entity.eventId
            name = entity.eventName
        }
    }

    override fun toEntity(model: EventProfileRoleCountModel): EventProfileRoleCountEntity {
        return EventProfileRoleCountEntity(
            eventId = model.event?.id,
            eventName = model.event?.name,
            level0 = model.level0,
        )
    }
}
