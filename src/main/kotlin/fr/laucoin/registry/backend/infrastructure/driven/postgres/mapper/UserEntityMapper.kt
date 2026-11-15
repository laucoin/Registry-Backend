package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class)
interface UserEntityMapper: IEntityMapper<UserModel, UserEntity> {
	@IgnoreModelAudit
	override fun toModel(entity: UserEntity): UserModel

	@IgnoreEntityAudit
	override fun toEntity(model: UserModel): UserEntity
}
