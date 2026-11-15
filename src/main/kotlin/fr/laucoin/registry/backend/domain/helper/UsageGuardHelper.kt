package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.CONFLICT
import reactor.core.publisher.Mono

/**
 * Objectif : Refuse la suppression d'un élément encore utilisé par d'autres éléments (mouvements, communications…).
 * Périmètre : Compte les éléments liés avec la requête fournie par l'appelant et lève un conflit (409) s'il en existe.
 * Limites : Ne verrouille pas l'élément ; l'appelant le verrouille avant de compter pour éviter une course avec un lien créé en parallèle.
 */
object UsageGuardHelper {
	private val log = LoggerFactory.getLogger(UsageGuardHelper::class.java)

	fun <T : GenericModel> Mono<T>.rejectIfStillUsed(error: String, count: (T) -> Mono<Long>): Mono<T> =
		flatMap { rejectIfUsed(it, error, count) }

	private fun <T : GenericModel> rejectIfUsed(element: T, error: String, count: (T) -> Mono<Long>): Mono<T> {
		return count(element)
			.handle { used, sink -> sink.nextOrError(element, usageError(element, used, error)) }
	}

	private fun usageError(element: GenericModel, used: Long, error: String): RegistryException? {
		if (used <= 0) return null
		log.warn("The element {} is still used by {} other element(s)", element.id, used)
		return RegistryException(CONFLICT, error)
	}
}
