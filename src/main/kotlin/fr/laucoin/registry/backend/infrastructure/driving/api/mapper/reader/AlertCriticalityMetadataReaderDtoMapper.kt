package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertCriticalityMetadataReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.AlertCriticalityRuleHelper
import org.springframework.stereotype.Component

@Component
class AlertCriticalityMetadataReaderDtoMapper(
	private val criticalityMapper: AlertCriticalityReaderDtoMapper,
	private val ruleHelper: AlertCriticalityRuleHelper,
): IGenericReaderDtoMapper<List<AlertCriticalityEnum>, AlertCriticalityMetadataReaderDto> {
	override fun toDto(model: List<AlertCriticalityEnum>): AlertCriticalityMetadataReaderDto {
		return AlertCriticalityMetadataReaderDto(
			levels = model.map(criticalityMapper::toDto),
			rule = ruleHelper.criticalityRule(model),
		)
	}
}
