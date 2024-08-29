package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IParticipantEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class
ParticipantModelPostgresRepository(
    private val repository: IParticipantEntityRepository,
    private val mapper: ParticipantEntityMapper,
): IParticipantModelRepository {
    override fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<ParticipantModel> {
        return repository.findAll(eventId, onlyVisible, startDateTime, endDateTime).map(mapper::toModel)
    }

    override fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<ParticipantModel> {
        return repository.findById(eventId, id, onlyVisible).map(mapper::toModel)
    }

    override fun save(element: ParticipantModel): Mono<ParticipantModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
