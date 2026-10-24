package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_RULE_KEY
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_RULE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertCriticalityMetadataReaderDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Purpose: Builds the alert criticality metadata: the levels and the sentence explaining how they are computed.
 * Scope: Reads the configured age thresholds so the displayed rule always matches the computed criticality.
 * Limits: Does not compute any criticality; the age thresholds are applied by the alert queries.
 */
@Component
class AlertCriticalityMetadataReaderDtoMapper(
	private val criticalityMapper: AlertCriticalityReaderDtoMapper,
	private val translateService: ITranslateService,
	@param:Value($$"${registry.feature.alert.criticality.reinforced-after-hours}")
	private val reinforcedAfterHours: Int,
	@param:Value($$"${registry.feature.alert.criticality.high-after-hours}")
	private val highAfterHours: Int,
): IGenericReaderDtoMapper<List<AlertCriticalityEnum>, AlertCriticalityMetadataReaderDto> {
	override fun toDto(model: List<AlertCriticalityEnum>): AlertCriticalityMetadataReaderDto {
		return AlertCriticalityMetadataReaderDto(
			levels = model.map(criticalityMapper::toDto),
			rule = translateService.getMessage(ALERT_CRITICALITY_RULE_KEY, arrayOf(levelsDescription(model))),
		)
	}

	private fun levelsDescription(levels: List<AlertCriticalityEnum>): String {
		return levels.joinToString(LEVELS_SEPARATOR) {
			translateService.getMessage("$ALERT_CRITICALITY_RULE_PREFIX$it", arrayOf(reinforcedAfterHours, highAfterHours))
		}
	}

	private companion object {
		const val LEVELS_SEPARATOR = " \u00B7 "
	}
}
