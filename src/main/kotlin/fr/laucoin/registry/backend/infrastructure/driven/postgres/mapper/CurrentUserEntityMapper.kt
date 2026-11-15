package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.CurrentUserMappingHelper
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class, uses = [CurrentUserMappingHelper::class])
interface CurrentUserEntityMapper: IEntityReaderMapper<CurrentUserModel, CurrentUserEntity> {
	@IgnoreModelAudit
	@Mapping(target = "preferences", source = "entity", qualifiedByName = ["preferencesOf"])
	@Mapping(target = "authorities", ignore = true)
	override fun toModel(entity: CurrentUserEntity): CurrentUserModel
}
