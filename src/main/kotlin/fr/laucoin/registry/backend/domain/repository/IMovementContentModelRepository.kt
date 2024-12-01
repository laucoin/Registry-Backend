package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.MovementContentModel
import reactor.core.publisher.Flux

interface IMovementContentModelRepository: IGenericWriteModelRepository<MovementContentModel> {
    fun saveAll(elements: List<MovementContentModel>): Flux<MovementContentModel>
}
