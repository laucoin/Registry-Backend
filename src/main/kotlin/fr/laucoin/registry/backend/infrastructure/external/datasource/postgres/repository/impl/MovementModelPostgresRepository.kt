package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class MovementModelPostgresRepository(
    private val repository: IMovementEntityRepository,
    private val mapper: MovementEntityMapper,
): IMovementModelRepository {
    override fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<MovementModel> = repository.findAll(eventId, onlyVisible, type, startDateTime, endDateTime).map(mapper::toModel)

    override fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<MovementModel> {
        return repository.findById(eventId, id, onlyVisible).map(mapper::toModel)
    }

    override fun save(element: MovementModel): Mono<MovementModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
