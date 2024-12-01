package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.MovementContentModel
import fr.laucoin.registry.backend.domain.repository.IMovementContentModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementContentEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class MovementContentModelPostgresRepository(
    private val repository: IMovementContentEntityRepository,
    private val mapper: MovementContentEntityMapper,
): IMovementContentModelRepository {
    override fun saveAll(elements: List<MovementContentModel>): Flux<MovementContentModel> {
        return repository.saveAll(elements.map(mapper::toEntity)).map(mapper::toModel)
    }

    override fun save(element: MovementContentModel): Mono<MovementContentModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
