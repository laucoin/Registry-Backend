package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import reactor.core.publisher.Mono

/**
 * Common base for domain services: the [updateVisibility] helper used by every disable/enable
 * use-case, the shared purge-deletion concurrency constant, and (via [LoggerService]) a ready-to-use
 * logger. Carries no persistence or business logic of its own.
 */
open class GenericService : LoggerService() {
	fun <T : GenericModel> Mono<T>.updateVisibility(isVisible: Boolean): Mono<T> {
		return this.map { it.apply { this.isVisible = isVisible } }
	}

	companion object {
		const val PURGE_DELETE_CONCURRENCY = 8
	}
}
