package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferencesEntity
import org.mapstruct.Mapper

@Mapper(config = PostgresMapperConfig::class)
interface PreferencesEntityMapper: IEntityMapper<PreferencesModel, PreferencesEntity> {
	@IgnoreModelAudit
	override fun toModel(entity: PreferencesEntity): PreferencesModel

	@IgnoreEntityAudit
	override fun toEntity(model: PreferencesModel): PreferencesEntity
}
