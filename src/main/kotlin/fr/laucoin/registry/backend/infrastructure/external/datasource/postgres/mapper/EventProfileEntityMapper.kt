package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndModel
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class EventProfileEntityMapper: IEntityMapper<EventProfileModel, EventProfileEntity> {
    override fun toModel(entity: EventProfileEntity): EventProfileModel {
        return EventProfileModel().apply {
            user = mapUserEntity(entity)
            role = entity.role
            status = entity.status
            startAccess = entity.startAccess
            endAccess = entity.endAccess
        }.fillWithEventAndEntity(entity)
    }

    private fun mapUserEntity(entity: EventProfileEntity): UserModel? {
        return if (Objects.isNull(entity.userId)) null
        else UserModel().apply {
            id = entity.userId
            firstName = entity.userFirstName
            lastName = entity.userLastName
            email = entity.userEmail
            lastLogin = entity.userLastLogin
            purged = entity.userPurged ?: purged
        }
    }

    override fun toEntity(model: EventProfileModel): EventProfileEntity {
        return EventProfileEntity().apply {
            userId = model.user?.id
            role = model.role
            status = model.status
            startAccess = model.startAccess
            endAccess = model.endAccess
        }.fillWithEventAndModel(model)
    }
}
