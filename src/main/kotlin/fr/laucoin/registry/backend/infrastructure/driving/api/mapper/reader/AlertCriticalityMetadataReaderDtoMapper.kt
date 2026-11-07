package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AlertCriticalityMetadataModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertCriticalityMetadataReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.AlertCriticalityRuleHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [AlertCriticalityReaderDtoMapper::class, AlertCriticalityRuleHelper::class])
interface AlertCriticalityMetadataReaderDtoMapper: IGenericReaderDtoMapper<AlertCriticalityMetadataModel, AlertCriticalityMetadataReaderDto> {
	@Mapping(target = "levels", source = "model.levels")
	@Mapping(target = "rule", source = "model.levels", qualifiedByName = ["criticalityRule"])
	override fun toDto(model: AlertCriticalityMetadataModel): AlertCriticalityMetadataReaderDto
}
