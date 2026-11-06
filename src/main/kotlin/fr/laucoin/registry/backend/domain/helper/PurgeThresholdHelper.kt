package fr.laucoin.registry.backend.domain.helper

import java.time.Clock
import java.time.LocalDate
import org.springframework.stereotype.Component

/**
 * Objectif : Résout la date seuil d'une purge de rétention.
 * Périmètre : Retient la date explicite si elle est fournie, sinon la date du jour moins le nombre de mois configuré.
 * Limites : Ne supprime rien ; il ne calcule que la date.
 */
@Component
class PurgeThresholdHelper(private val clock: Clock) {
	fun resolve(monthsThreshold: Long, dateThreshold: LocalDate?): LocalDate {
		return dateThreshold ?: LocalDate.now(clock).minusMonths(monthsThreshold)
	}
}
