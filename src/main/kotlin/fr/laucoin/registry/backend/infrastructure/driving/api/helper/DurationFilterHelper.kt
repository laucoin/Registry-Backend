package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.springframework.http.HttpStatus.BAD_REQUEST
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Objectif : Convertit un filtre de durée ISO 8601 reçu en paramètre de requête en Duration.
 * Périmètre : Refuse par une erreur de requête un format invalide, une durée négative, infinie ou supérieure à un an.
 * Limites : Ne compare pas les bornes minimale et maximale entre elles ; le modèle de recherche s'en charge.
 */
object DurationFilterHelper {
	private val MAX_FILTER_DURATION: Duration = 366.days

	fun parseDurationFilter(duration: String?): Duration? {
		val parsed = try {
			duration?.let(Duration::parse)
		} catch (e: IllegalArgumentException) {
			throw RegistryException(BAD_REQUEST, ACTIVITY_DURATION_FORMAT_FAILED)
		}

		if (parsed != null && (!parsed.isFinite() || parsed.isNegative() || parsed > MAX_FILTER_DURATION)) {
			throw RegistryException(BAD_REQUEST, ACTIVITY_DURATION_FORMAT_FAILED)
		}
		return parsed
	}
}
