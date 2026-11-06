package fr.laucoin.registry.backend.domain.helper

import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Objectif : Supprime une à une les ressources éligibles d'un balayage de rétention.
 * Périmètre : Applique le mode simulation, borne la concurrence des suppressions (séquentielle à 1) et journalise chaque ressource traitée.
 * Limites : Ne sélectionne pas les ressources éligibles et n'ouvre aucune transaction ; l'appelant s'en charge.
 */
object PurgeHelper {
	const val PURGE_DELETE_CONCURRENCY = 8

	private val log = LoggerFactory.getLogger(PurgeHelper::class.java)

	fun Flux<UUID>.purgeEach(
		label: String,
		dryRun: Boolean,
		delete: (UUID) -> Mono<Unit>,
		concurrency: Int = PURGE_DELETE_CONCURRENCY,
	): Flux<UUID> {
		return flatMap({ purgeOne(it, label, dryRun, delete) }, concurrency)
	}

	private fun purgeOne(id: UUID, label: String, dryRun: Boolean, delete: (UUID) -> Mono<Unit>): Mono<UUID> {
		if (dryRun) {
			log.info("[Dry run] {} {} would be deleted", label, id)
			return Mono.just(id)
		}

		log.info("Purging {} {}", label, id)
		return delete(id).thenReturn(id)
			.doOnNext { log.info("{} {} was deleted", label, it) }
			.doOnError { log.error("Failed to purge {} {}", label, id, it) }
	}
}
