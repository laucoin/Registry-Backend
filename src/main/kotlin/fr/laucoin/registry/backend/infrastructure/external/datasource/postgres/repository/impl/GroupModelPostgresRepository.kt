package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.GroupEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IGroupEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class GroupModelPostgresRepository(
    private val repository: IGroupEntityRepository,
    private val contentRepository: IGroupContentEntityRepository,
    private val transactionalOperator: TransactionalOperator,
    private val mapper: GroupEntityMapper,
    private val contentMapper: GroupContentEntityMapper,
): IGroupModelRepository {
    override fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<GroupModel> = repository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime).map(mapper::toModel)

    override fun findAllByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<GroupModel> {
        return if (ids.isEmpty()) Flux.empty()
        else repository.findAllByIds(eventId, ids, onlyVisible).map(mapper::toModel)
    }

    override fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<GroupModel> {
        return repository.findById(eventId, id, onlyVisible).map(mapper::toModel)
    }

    override fun create(element: GroupModel): Mono<GroupModel> {
        return save(element)
            .saveNewMembers(element)
            .`as`(transactionalOperator::transactional)
    }

    override fun update(element: GroupModel): Mono<GroupModel> {
        return save(element)
            .flatMap { findById(element.event !!.id !!, element.id !!, onlyVisible = false) }
            .saveNewMembers(element)
            .removeDeletedMembers(element)
            .`as`(transactionalOperator::transactional)
    }

    @Transactional
    fun Mono<GroupModel>.saveNewMembers(element: GroupModel): Mono<GroupModel> {
        return flatMap { group ->
            val newMembers = group.getNewMembers(element)
            if (newMembers.isEmpty()) return@flatMap Mono.just(group)
            contentRepository.saveAll(newMembers.map { contentMapper.toEntity(group.id !!, it) })
                .map(contentMapper::toModel)
                .collectList()
                .map { group.apply { members = members.plus(it) } }
        }
    }

    @Transactional
    fun Mono<GroupModel>.removeDeletedMembers(element: GroupModel): Mono<GroupModel> {
        return flatMap { group ->
            val removedMembers = group.getRemovedMemberIds(element)
            if (removedMembers.isEmpty()) return@flatMap Mono.just(group)
            contentRepository.deleteAllByGroupIdAndParticipantIds(group.id !!, removedMembers)
                .then(Mono.fromCallable { group.apply { members = members.filter { removedMembers.contains(it.id) } } })
        }
    }

    private fun save(element: GroupModel): Mono<GroupModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
