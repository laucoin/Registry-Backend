package fr.laucoin.registry.backend.infrastructure.driven.postgres.extension

import java.time.Clock
import java.time.ZonedDateTime
import org.springframework.data.spel.spi.EvaluationContextExtension
import org.springframework.stereotype.Component

/**
 * Objectif : Expose l'horloge de l'application aux expressions SpEL des requêtes des repositories.
 * Périmètre : Donne aux requêtes l'instant courant via `:#{registryClock.now}` au lieu de l'horloge de la base de données.
 * Limites : Ne fournit que l'instant ; la gestion du fuseau horaire reste dans les requêtes.
 */
@Component
class ClockEvaluationContextExtension(private val clock: Clock): EvaluationContextExtension {
	override fun getExtensionId(): String = "registryClock"

	override fun getRootObject(): Any = ClockRoot(clock)

	class ClockRoot(private val clock: Clock) {
		val now: ZonedDateTime get() = ZonedDateTime.now(clock)
	}
}
