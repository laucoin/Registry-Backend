package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import reactor.core.publisher.Mono

open class GenericService : LoggerService() {
	fun <T : GenericModel> Mono<T>.updateVisibility(visibility: Boolean): Mono<T> {
		return this.map { it.apply { visible = visibility } }
	}
}
