package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import kotlin.time.Duration
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.stereotype.Component

/**
 * Objectif : Convertit la durée texte d'une activité reçue par l'API en Duration, valeur Kotlin illisible par MapStruct.
 * Périmètre : Renseigne la durée du modèle après le mapping et refuse un format invalide par une erreur de requête.
 * Limites : Ne valide pas la durée au-delà de son format.
 */
@Component
class ActivityDurationMappingHelper {
	@AfterMapping
	fun fillDuration(dto: ActivityWriterDto, @MappingTarget model: ActivityModel) {
		model.duration = try {
			dto.duration?.let(Duration::parse)
		} catch (e: Exception) {
			throw RegistryException(status = BAD_REQUEST, code = ACTIVITY_DURATION_FORMAT_FAILED)
		}
	}
}
