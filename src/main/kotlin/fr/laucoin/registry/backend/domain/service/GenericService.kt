package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.DESC
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

open class GenericService: LoggerService() {
    fun <T: GenericModel> Flux<T>.sort(order: Direction, comparator: Comparator<T>? = null): Flux<T> {
        return this.sort(comparator !!.let { if (order == DESC) it.reversed() else it })
    }

    fun <T: GenericModel> Mono<T>.updateVisibility(visibility: Boolean): Mono<T> {
        return this.map { it.apply { visible = visibility } }
    }
}
