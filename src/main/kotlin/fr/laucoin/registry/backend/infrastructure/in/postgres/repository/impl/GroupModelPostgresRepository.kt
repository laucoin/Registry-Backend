package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.GroupEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IGroupEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
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
): IGroupPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
	): Mono<PageModel<GroupModel>> {
		return Mono.zip(
			repository.countAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.presenceSearched,
				searchParams.dateTimeSearched,
			),
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.presenceSearched,
				searchParams.dateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList(),
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findContent(
		projectId: UUID,
		groupIds: List<UUID>,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
	): Flux<Pair<UUID, List<ParticipantModel>>> {
		return if (groupIds.isEmpty()) Flux.empty()
		else contentRepository.findAllByGroupIds(projectId, groupIds, visibilitySearched, availabilitySearched)
			.groupBy(GroupContentEntity::groupId)
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(contentMapper::toModel) }
			}
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<GroupModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, visibilitySearched)
			.map(mapper::toModel)
	}

	override fun findWithLimit(limit: Int, projectId: UUID, searchParams: GroupSearchParamModel): Flux<GroupModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findEmpty(participantToExclude: List<UUID>): Flux<UUID> {
		return if (participantToExclude.isEmpty()) Flux.empty()
		else repository.findEmpty(participantToExclude)
	}

	override fun findByIdWithContent(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
		memberVisibilitySearched: Boolean?,
		memberAvailabilitySearched: Boolean?,
	): Mono<GroupModel> {
		return Mono.zip(
			repository.findById(projectId, id, visibilitySearched).map(mapper::toModel),
			findContent(projectId, listOf(id), memberVisibilitySearched, memberAvailabilitySearched).collectList()
				.handle { it, handle -> if (it.isNullOrEmpty()) handle.next(emptyList()) else handle.next(it.first().second) }
		).map {
			it.t1.members = it.t2
			it.t1
		}
	}

	override fun create(element: GroupModel): Mono<GroupModel> {
		return save(element)
			.saveNewMembers(element)
			.`as`(transactionalOperator::transactional)
	}

	override fun update(element: GroupModel): Mono<GroupModel> {
		return save(element)
			.flatMap {
				findByIdWithContent(
					element.project!!.id!!,
					element.id!!,
					visibilitySearched = null,
					memberVisibilitySearched = null,
					memberAvailabilitySearched = null,
				)
			}
			.removeDeletedMembers(element)
			.saveNewMembers(element)
			.`as`(transactionalOperator::transactional)
	}

	fun Mono<GroupModel>.saveNewMembers(element: GroupModel): Mono<GroupModel> {
		return flatMap { group ->
			val newMembers = group.getNewMembers(element)
			if (newMembers.isEmpty()) return@flatMap Mono.just(group)
			contentRepository.saveAll(newMembers.map { contentMapper.toEntity(group.id!!, it) })
				.map(contentMapper::toModel)
				.collectList()
				.map { group.apply { members = members.plus(it) } }
		}
	}

	fun Mono<GroupModel>.removeDeletedMembers(element: GroupModel): Mono<GroupModel> {
		return flatMap { group ->
			val removedMembers = group.getOldMemberIds(element)
			if (removedMembers.isEmpty()) return@flatMap Mono.just(group)
			contentRepository.deleteAllByGroupIdAndParticipantIds(group.id!!, removedMembers)
				.then(Mono.fromCallable {
					group.apply {
						members = members.filter { !removedMembers.contains(it.id) }
					}
				})
		}
	}

	private fun save(element: GroupModel): Mono<GroupModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
