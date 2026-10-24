package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.model.GenericModel
import java.time.Instant
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Protège une mise à jour avec la précondition optimiste envoyée par le client.
 * Périmètre : Verrouille la ligne de la ressource, compare sa dernière édition aux éditions attendues et exécute la mise à jour dans la même transaction.
 * Limites : Sans dernière édition attendue, la mise à jour s'exécute sans garde ; elle ne fusionne jamais des modifications en conflit.
 */
interface IConcurrencyService {
	fun <M : GenericModel, R : Any> updateIfUnchanged(
		resource: LockableResourceEnum,
		id: UUID,
		expectedLastEditions: List<Instant>?,
		current: () -> Mono<M>,
		action: () -> Mono<R>,
	): Mono<R>
}
