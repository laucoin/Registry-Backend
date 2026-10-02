package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Small Reactor helpers shared across services: [notFoundIfEmpty] turns an empty `Mono` into the
 * standard 404 [RegistryException] (logging the identifier/permissions context), and [toPageModel]
 * assembles a [PageModel] from a `Flux` of rows carrying their own total-count column.
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

	fun <E : Any, T> Flux<E>.toPageModel(
		pageable: PageableModel,
		fullCount: (E) -> Long?,
		toModel: (E) -> T,
	): Mono<PageModel<T>> {
		return collectList().map { entities ->
			PageModel(pageable, entities.firstOrNull()?.let(fullCount) ?: 0L, entities.map(toModel))
		}
	}
}
