package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.IConcurrencyService
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Protège une mise à jour avec la précondition optimiste envoyée par le client.
 * Périmètre : Verrouille la ligne de la ressource, compare sa dernière édition aux éditions attendues et exécute la mise à jour dans la même transaction.
 * Limites : Sans dernière édition attendue, la mise à jour s'exécute sans garde ; elle ne fusionne jamais des modifications en conflit.
 */
@Service
class ConcurrencyService(
	private val rowLockPort: IRowLockPort,
	private val transactionalOperator: TransactionalOperator,
) : IConcurrencyService, LoggerService() {
	override fun <M : GenericModel, R : Any> updateIfUnchanged(
		resource: LockableResourceEnum,
		id: UUID,
		expectedLastEditions: List<Instant>?,
		current: () -> Mono<M>,
		action: () -> Mono<R>,
	): Mono<R> {
		if (expectedLastEditions == null) return action()

		return rowLockPort.lock(resource, id)
			.then(Mono.defer(current))
			.map { Optional.of(it) }
			.defaultIfEmpty(Optional.empty())
			.flatMap { checkThenAct(resource, id, it, expectedLastEditions, action) }
			.`as`(transactionalOperator::transactional)
	}

	private fun <M : GenericModel, R : Any> checkThenAct(
		resource: LockableResourceEnum,
		id: UUID,
		current: Optional<M>,
		expectedLastEditions: List<Instant>,
		action: () -> Mono<R>,
	): Mono<R> {
		val lastEdition = current.map { it.lastEdition?.dateTime?.toInstant()?.toEpochMilli() }.orElse(null)

		return if (lastEdition != null && lastEdition !in expectedLastEditions.map(Instant::toEpochMilli)) {
			log.warn("The {} {} was modified since it was read", resource, id)
			Mono.error(RegistryException(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ))
		} else {
			action()
		}
	}
}
