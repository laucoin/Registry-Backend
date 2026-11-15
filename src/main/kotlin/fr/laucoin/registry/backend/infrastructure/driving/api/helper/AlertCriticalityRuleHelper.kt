package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_RULE_KEY
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_RULE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import org.mapstruct.Named
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Objectif : Compose le texte de la règle de criticité des alertes.
 * Périmètre : Décrit chaque niveau avec les seuils configurés en heures, dans la langue de la requête.
 * Limites : Ne lit aucune alerte.
 */
@Component
class AlertCriticalityRuleHelper(
	private val translateService: ITranslateService,
	@param:Value($$"${registry.feature.alert.criticality.reinforced-after-hours}")
	private val reinforcedAfterHours: Int,
	@param:Value($$"${registry.feature.alert.criticality.high-after-hours}")
	private val highAfterHours: Int,
) {
	private companion object {
		const val LEVELS_SEPARATOR = " \u00B7 "
	}

	@Named("criticalityRule")
	fun criticalityRule(levels: List<AlertCriticalityEnum>): String {
		return translateService.getMessage(ALERT_CRITICALITY_RULE_KEY, arrayOf(levelsDescription(levels)))
	}

	private fun levelsDescription(levels: List<AlertCriticalityEnum>): String {
		return levels.joinToString(LEVELS_SEPARATOR) {
			translateService.getMessage("$ALERT_CRITICALITY_RULE_PREFIX$it", arrayOf(reinforcedAfterHours, highAfterHours))
		}
	}
}
