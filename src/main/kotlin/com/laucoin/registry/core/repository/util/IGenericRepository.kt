package com.laucoin.registry.core.repository.util

import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@NoRepositoryBean
interface IGenericRepository<T, ID>: ReactiveCrudRepository<T, ID> {
    fun getAll(onlyVisible: Boolean): Flux<T>
    fun findById(id: ID, onlyVisible: Boolean): Mono<T>
}
