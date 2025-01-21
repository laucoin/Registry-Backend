package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithModel
import org.springframework.stereotype.Component

@Component
class UserEntityMapper: IEntityMapper<UserModel, UserEntity> {
    override fun toModel(entity: UserEntity): UserModel {
        return UserModel().apply {
            oidcId = entity.oidcId
            type = entity.type ?: type
            firstName = entity.firstName
            lastName = entity.lastName
            email = entity.email
            role = entity.role
            birthday = entity.birthday
            lastLogin = entity.lastLogin
            purged = entity.purged ?: purged
        }.fillWithEntity(entity)
    }

    override fun toEntity(model: UserModel): UserEntity {
        return UserEntity().apply {
            oidcId = model.oidcId
            type = model.type
            firstName = model.firstName
            lastName = model.lastName
            email = model.email
            role = model.role
            birthday = model.birthday
            lastLogin = model.lastLogin
            purged = model.purged
        }.fillWithModel(model)
    }
}
