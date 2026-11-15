package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import java.time.Duration
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Expose la durée d'une activité sous forme de libellé pour l'API.
 * Périmètre : Convertit la Duration Kotlin du modèle en libellé lisible et en durée ISO 8601.
 * Limites : Ne modifie pas l'activité.
 */
@Component
class ActivityLabelMappingHelper {
	@Named("activityDuration")
	fun activityDuration(model: ActivityModel): LabelDto? {
		return model.duration?.let { LabelDto(label = it.toString(), value = it.toIsoString()) }
	}
}
