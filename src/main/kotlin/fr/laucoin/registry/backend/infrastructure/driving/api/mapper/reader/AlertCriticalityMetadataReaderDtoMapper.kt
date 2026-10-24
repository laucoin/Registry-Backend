package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_RULE_KEY
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_RULE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertCriticalityMetadataReaderDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Objectif : Construit les métadonnées de criticité des alertes : les niveaux et la phrase expliquant leur calcul.
 * Périmètre : Lit les seuils d'ancienneté configurés pour que la règle affichée corresponde toujours à la criticité calculée.
 * Limites : Ne calcule aucune criticité ; les seuils d'ancienneté sont appliqués par les requêtes des alertes.
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
