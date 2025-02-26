package fr.laucoin.registry.backend.domain.repository

import java.util.UUID
import reactor.core.publisher.Mono

interface IGenericReadModelRepository<T> {
    fun findById(id: UUID, visibilitySearched: Boolean?): Mono<T>
}
