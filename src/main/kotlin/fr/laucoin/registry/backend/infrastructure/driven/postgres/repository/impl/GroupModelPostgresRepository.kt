package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GroupContentJooqRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GroupJooqRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupModelPostgresRepository(
	private val repository: GroupJooqRepository,
	private val contentRepository: GroupContentJooqRepository,
	private val dsl: DSLContext,
	private val mapper: GroupEntityMapper,
	private val contentMapper: GroupContentEntityMapper,
) : IGroupPort {
	override fun findAllByCreatorId(userId: UUID): Flux<GroupModel> {
		return repository.findAllByCreatorId(userId).map(mapper::toModel)
	}

	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
		sortFields: List<SortModel<GroupSortFieldEnum>>,
	): Mono<PageModel<GroupModel>> {
		return repository.findAll(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
			sortFields,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, GroupEntity::fullCount, mapper::toModel)
	}

	override fun findContent(
		projectId: UUID,
		groupIds: List<UUID>,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
	): Flux<Pair<UUID, List<ParticipantModel>>> {
		return if (groupIds.isEmpty()) Flux.empty()
		else contentRepository.findAllByGroupIds(projectId, groupIds, visibilitySearched, availabilitySearched)
			.groupBy { it.groupId!! }
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

	override fun findArrivingToday(projectId: UUID, limit: Int): Flux<GroupModel> {
		return repository.findArrivingToday(projectId, limit).map(mapper::toModel)
	}

	override fun findDepartingToday(projectId: UUID, limit: Int): Flux<GroupModel> {
		return repository.findDepartingToday(projectId, limit).map(mapper::toModel)
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
				.handle<List<ParticipantModel>> { it, handle ->
					if (it.isNullOrEmpty()) handle.next(emptyList()) else handle.next(
						it.first().second
					)
				}
		).map {
			it.t1.members = it.t2
			it.t1
		}
	}

	// Atomicity across the save + members diff is the caller's responsibility (`TransactionalOperator`),
	// not this repository's — see the jOOQ-vs-Spring-transaction note on `JooqConfig.dslContext()`.
	override fun create(element: GroupModel): Mono<GroupModel> {
		return save(dsl, element).saveNewMembers(dsl, element)
	}

	override fun update(element: GroupModel): Mono<GroupModel> {
		return save(dsl, element)
			.flatMap { findByIdWithContent(dsl, element.project!!.id!!, element.id!!) }
			.removeDeletedMembers(dsl, element)
			.saveNewMembers(dsl, element)
	}

	private fun findByIdWithContent(using: DSLContext, projectId: UUID, id: UUID): Mono<GroupModel> {
		return Mono.zip(
			repository.findById(projectId, id, visibilitySearched = null, using = using).map(mapper::toModel),
			contentRepository.findAllByGroupIds(
				projectId,
				listOf(id),
				visibilitySearched = null,
				availabilitySearched = null,
				using = using,
			).map(contentMapper::toModel).collectList(),
		).map {
			it.t1.members = it.t2
			it.t1
		}
	}

	fun Mono<GroupModel>.saveNewMembers(using: DSLContext, element: GroupModel): Mono<GroupModel> {
		return flatMap { group ->
			val newMembers = group.getNewMembers(element)
			if (newMembers.isEmpty()) return@flatMap Mono.just(group)
			contentRepository.saveAll(newMembers.map { contentMapper.toEntity(group.id!!, it) }, using)
				.map(contentMapper::toModel)
				.collectList()
				.map { group.apply { members = members.plus(it) } }
		}
	}

	fun Mono<GroupModel>.removeDeletedMembers(using: DSLContext, element: GroupModel): Mono<GroupModel> {
		return flatMap { group ->
			val removedMembers = group.getOldMemberIds(element)
			if (removedMembers.isEmpty()) return@flatMap Mono.just(group)
			contentRepository.deleteAllByGroupIdAndParticipantIds(group.id!!, removedMembers, using)
				.then(Mono.fromCallable {
					group.apply {
						members = members.filter { !removedMembers.contains(it.id) }
					}
				})
		}
	}

	private fun save(using: DSLContext, element: GroupModel): Mono<GroupModel> {
		return repository.save(mapper.toEntity(element), using).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
