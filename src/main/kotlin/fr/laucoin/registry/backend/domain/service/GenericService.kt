package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.GenericModel
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Objectif : Classe de base des services, avec les utilitaires qu'ils partagent.
 * Périmètre : Fournit l'instant courant issu de l'horloge injectée et les mises à jour de visibilité.
 * Limites : Ne porte aucun cas d'usage propre.
 */
open class GenericService(protected val clock: Clock) : LoggerService() {
	fun now(): ZonedDateTime = ZonedDateTime.now(clock)

	fun <T : GenericModel> Mono<T>.updateVisibility(visibility: Boolean): Mono<T> {
		return this.map { it.apply { visible = visibility } }
	}
}
