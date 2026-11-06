package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import org.mapstruct.Named
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class)
interface AlertEntityMapper: IEntityMapper<AlertModel, AlertEntity> {
	@IgnoreModelAudit
	@Mapping(target = "communications", ignore = true)
	@Mapping(target = "lastCommunication", source = "entity", qualifiedByName = ["lastCommunicationOf"])
	override fun toModel(entity: AlertEntity): AlertModel

	@IgnoreEntityAudit
	@Mapping(target = "criticality", ignore = true)
	@Mapping(target = "lastCommunicationDateTime", ignore = true)
	@Mapping(target = "lastCommunicationMessage", ignore = true)
	override fun toEntity(model: AlertModel): AlertEntity

	@Named("lastCommunicationOf")
	fun lastCommunicationOf(entity: AlertEntity): CommunicationModel? {
		return entity.lastCommunicationDateTime?.let { CommunicationModel(it, entity.lastCommunicationMessage) }
	}
}
