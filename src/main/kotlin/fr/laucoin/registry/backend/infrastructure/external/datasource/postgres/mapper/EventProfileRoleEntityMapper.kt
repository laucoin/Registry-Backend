package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.EventProfileRoleModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileRoleEntity
import org.springframework.stereotype.Component

@Component
class EventProfileRoleEntityMapper: IEntityReaderMapper<EventProfileRoleModel, EventProfileRoleEntity> {
    override fun toModel(entity: EventProfileRoleEntity): EventProfileRoleModel {
        return EventProfileRoleModel(
            role = entity.role,
            eventId = entity.eventId,
            eventOptions = entity.eventOptions,
            eventVisible = entity.eventVisible
        )
    }
}
