package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.CommunicationMappingHelper
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class, uses = [CommunicationMappingHelper::class])
interface CommunicationEntityMapper: IEntityMapper<CommunicationModel, CommunicationEntity> {
	@IgnoreModelAudit
	@Mapping(target = "movement", source = "entity", qualifiedByName = ["movementOf"])
	@Mapping(target = "alert", source = "entity", qualifiedByName = ["alertOf"])
	override fun toModel(entity: CommunicationEntity): CommunicationModel

	@BeanMapping(ignoreByDefault = true)
	@KeepGenericProjectColumns
	@Mapping(target = "dateTime", source = "dateTime")
	@Mapping(target = "message", source = "message")
	@Mapping(target = "movementId", source = "movement.id")
	@Mapping(target = "alertId", source = "alert.id")
	override fun toEntity(model: CommunicationModel): CommunicationEntity
}
