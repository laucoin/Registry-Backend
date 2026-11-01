package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import java.time.Clock
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupOnSiteCountsEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IGroupEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port du groupe.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class GroupModelPostgresRepository(
	private val repository: IGroupEntityRepository,
	private val contentRepository: IGroupContentEntityRepository,
	private val transactionalOperator: TransactionalOperator,
	private val mapper: GroupEntityMapper,
	private val contentMapper: GroupContentEntityMapper,
	private val clock: Clock,
): IGroupPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
	): Mono<PageModel<GroupModel>> {
		return repository.findAll(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isPresent,
			searchParams.at,
			searchParams.memberUserId,
			searchParams.hasOnSiteMembers,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, GroupEntity::fullCount, mapper::toModel)
			.flatMap { withOnSiteCounts(projectId, it) }
	}

	private fun withOnSiteCounts(projectId: UUID, page: PageModel<GroupModel>): Mono<PageModel<GroupModel>> {
		val ids = page.content.mapNotNull { it.id }
		if (ids.isEmpty()) return Mono.just(page)
		return contentRepository.findOnSiteCounts(projectId, ids, at = null)
			.collectMap { it.groupId!! }
			.map { counts -> applyOnSiteCounts(page, counts) }
	}

	private fun applyOnSiteCounts(page: PageModel<GroupModel>, counts: Map<UUID, GroupOnSiteCountsEntity>): PageModel<GroupModel> {
		page.content.forEach { it.fillOnSiteCounts(counts[it.id]) }
		return page
	}

	private fun GroupModel.fillOnSiteCounts(counts: GroupOnSiteCountsEntity?) {
		onSiteMembersCount = counts?.onSiteMembers ?: 0
		inMembersCount = counts?.inMembers ?: 0
		outMembersCount = counts?.outMembers ?: 0
		overdueMembersCount = counts?.overdueMembers ?: 0
	}

	override fun findContent(
		projectId: UUID,
		groupIds: List<UUID>,
		isVisible: Boolean?,
		isAvailable: Boolean?,
	): Flux<Pair<UUID, List<ParticipantModel>>> {
		return if (groupIds.isEmpty()) Flux.empty()
		else contentRepository.findAllByGroupIds(projectId, groupIds, isVisible, isAvailable)
			.groupBy { it.groupId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(contentMapper::toModel) }
			}
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<GroupModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, isVisible)
			.map(mapper::toModel)
	}

	override fun findSummaryById(projectId: UUID, id: UUID): Mono<GroupModel> {
		return repository.findSummaryById(projectId, id).map(mapper::toModel)
	}

	override fun lockByIds(ids: List<UUID>): Flux<UUID> {
		return if (ids.isEmpty()) Flux.empty() else repository.lockByIds(ids)
	}

	override fun findWithLimit(limit: Int, projectId: UUID, searchParams: GroupSearchParamModel): Flux<GroupModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isPresent,
			searchParams.at,
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
		isVisible: Boolean?,
		memberIsVisible: Boolean?,
		memberIsAvailable: Boolean?,
	): Mono<GroupModel> {
		return Mono.zip(
			repository.findById(projectId, id, isVisible).map(mapper::toModel),
			findContent(projectId, listOf(id), memberIsVisible, memberIsAvailable).collectList()
				.handle<List<ParticipantModel>> { it, handle -> if (it.isNullOrEmpty()) handle.next(emptyList()) else handle.next(it.first().second) }
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
					element.projectId!!,
					element.id!!,
					isVisible = null,
					memberIsVisible = null,
					memberIsAvailable = null,
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
