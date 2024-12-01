package fr.laucoin.registry.backend.domain.repository

import java.util.UUID
import reactor.core.publisher.Mono

interface IGenericReadEventModelRepository<T> {
    fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<T>
}
