package fr.laucoin.registry.backend.domain.repository

import java.util.UUID
import reactor.core.publisher.Mono

interface IGenericWriteModelRepository<T> {
    fun create(element: T): Mono<T>
    fun update(element: T): Mono<T>
    fun deleteById(id: UUID): Mono<Void>
}
