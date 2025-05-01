package fr.laucoin.registry.backend.domain.repository

import java.util.UUID
import reactor.core.publisher.Mono

interface IGenericReadProjectModelRepository<T> {
    fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<T>
}
