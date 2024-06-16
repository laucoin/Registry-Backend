package com.laucoin.registry.core.repository.util

import com.laucoin.registry.core.model.util.GenericModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IGenericModelRepository<W: GenericModel, R: W> {
    fun getAll(onlyVisible: Boolean): Flux<R>
    fun findById(id: UUID, onlyVisible: Boolean): Mono<R>
    fun create(element: W): Mono<W>
    fun updateById(id: UUID, element: W): Mono<W>
    fun deleteById(id: UUID): Mono<Void>
}
