package fr.laucoin.registry.backend.domain.extension

import reactor.core.publisher.SynchronousSink
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Objectif : Fournit les extensions Reactor communes aux services et aux repositories.
 * Périmètre : Erreur « introuvable » sur flux vide, émission conditionnelle d'une valeur ou d'une erreur, construction d'une page.
 * Limites : Ne porte aucune règle métier ; aucune souscription ni blocage.
 */
object ReactiveExt {
	fun <T : Any> Mono<T>.notFoundIfEmpty(identifier: Any): Mono<T> {
		return switchIfEmpty {
			LoggerFactory.getLogger(this::class.java).warn(
				"Not data found with the given identifier ({}) and the current user permissions",
				identifier
			)
			throw RegistryException(
				status = NOT_FOUND,
				code = NOT_FOUND_WITH_GIVEN_IDENTIFIER,
				args = arrayListOf(identifier.toString()),
			)
		}
	}

	fun <T : Any> SynchronousSink<T>.nextOrError(value: T, error: Throwable?) {
		if (error == null) next(value) else error(error)
	}

	fun <E : Any, T> Flux<E>.toPageModel(
		pageable: PageableModel,
		clock: Clock,
		fullCount: (E) -> Long?,
		toModel: (E) -> T,
	): Mono<PageModel<T>> {
		return collectList().map { entities ->
			PageModel(pageable, entities.firstOrNull()?.let(fullCount) ?: 0L, entities.map(toModel), ZonedDateTime.now(clock))
		}
	}
}
