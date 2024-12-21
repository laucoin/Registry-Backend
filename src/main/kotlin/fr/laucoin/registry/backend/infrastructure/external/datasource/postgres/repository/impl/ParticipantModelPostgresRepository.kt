package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IParticipantEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ParticipantModelPostgresRepository(
    private val repository: IParticipantEntityRepository,
    private val groupContentRepository: IGroupContentEntityRepository,
    private val transactionalOperator: TransactionalOperator,
    private val mapper: ParticipantEntityMapper,
    private val groupContentMapper: GroupContentEntityMapper,
): IParticipantModelRepository {
    override fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<ParticipantModel> {
        return repository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime).map(mapper::toModel)
    }

    override fun findAllByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<ParticipantModel> {
        return if (ids.isEmpty()) Flux.empty()
        else repository.findAllByIds(eventId, ids, onlyVisible).map(mapper::toModel)
    }

    override fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<ParticipantModel> {
        return repository.findById(eventId, id, onlyVisible).map(mapper::toModel)
    }

    override fun create(element: ParticipantModel): Mono<ParticipantModel> {
        return save(element)
            .saveNewGroups(element)
            .`as`(transactionalOperator::transactional)
    }

    override fun update(element: ParticipantModel): Mono<ParticipantModel> {
        return save(element)
            .saveNewGroups(element)
            .removeDeletedGroups(element)
            .`as`(transactionalOperator::transactional)
    }

    @Transactional
    fun Mono<ParticipantModel>.saveNewGroups(element: ParticipantModel): Mono<ParticipantModel> {
        return flatMap { participant ->
            val newGroups = participant.getNewGroups(element)
            if (newGroups.isEmpty()) return@flatMap Mono.just(participant)
            groupContentRepository.saveAll(newGroups.map { groupContentMapper.toEntity(it.id !!, participant) })
                .map(groupContentMapper::toModel)
                .collectList()
                .map { participant.apply { groups = groups.plus(newGroups) } }
        }
    }

    @Transactional
    fun Mono<ParticipantModel>.removeDeletedGroups(element: ParticipantModel): Mono<ParticipantModel> {
        return flatMap { participant ->
            val removedGroups = participant.getRemovedGroupIds(element)
            if (removedGroups.isEmpty()) return@flatMap Mono.just(participant)
            groupContentRepository.deleteAllByParticipantIdAndGroupIds(participant.id !!, removedGroups)
                .then(Mono.fromCallable { participant.apply { groups = groups.filter { removedGroups.contains(it.id) } } })
        }
    }

    private fun save(element: ParticipantModel): Mono<ParticipantModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
