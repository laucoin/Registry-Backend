package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IParticipantEntityRepository
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port du participant.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class ParticipantModelPostgresRepository(
	private val repository: IParticipantEntityRepository,
	private val groupContentRepository: IGroupContentEntityRepository,
	private val transactionalOperator: TransactionalOperator,
	private val mapper: ParticipantEntityMapper,
	private val groupContentMapper: GroupContentEntityMapper,
	private val clock: Clock,
): IParticipantPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>> {
		return repository.findAll(
			projectId,
			searchParams.query,
			searchParams.isMajor,
			searchParams.type,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.presence,
			searchParams.at,
			searchParams.hasUser,
			searchParams.isOnSite,
			searchParams.birthdayFrom,
			searchParams.birthdayTo,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, ParticipantEntity::fullCount, mapper::toModel)
	}

	override fun findBirthdays(projectId: UUID, isVisible: Boolean?): Flux<ParticipantModel> {
		return repository.findAllWithBirthday(projectId, isVisible)
			.map(mapper::toModel)
	}

	override fun countAll(
		projectId: UUID,
		searchParams: ParticipantSearchParamModel
	): Mono<Long> {
		return repository.countAll(
			projectId,
			searchParams.query,
			searchParams.isMajor,
			searchParams.type,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.presence,
			searchParams.at,
			searchParams.hasUser,
			searchParams.isOnSite,
			searchParams.birthdayFrom,
			searchParams.birthdayTo,
		)
	}

	override fun findPageByGroupId(
		projectId: UUID,
		groupId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel
	): Mono<PageModel<ParticipantModel>> {
		return repository.findAllByGroupId(
			projectId,
			groupId,
			searchParams.query,
			searchParams.isMajor,
			searchParams.type,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.presence,
			searchParams.at,
			searchParams.hasUser,
			searchParams.isOnSite,
			searchParams.birthdayFrom,
			searchParams.birthdayTo,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, ParticipantEntity::fullCount, mapper::toModel)
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<ParticipantModel> {
		return if (ids.isEmpty()) Flux.empty()
		else repository.findAllByIds(projectId, ids, isVisible, at = null).map(mapper::toModel)
	}

	override fun findByUserId(projectId: UUID, userId: UUID): Flux<ParticipantModel> {
		return repository.findByUserId(projectId, userId, null).map(mapper::toModel)
	}

	override fun findAllByUserId(userId: UUID): Flux<ParticipantModel> {
		return repository.findAllByUserId(userId).map(mapper::toModel)
	}

	override fun findWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: ParticipantSearchParamModel
	): Flux<ParticipantModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.query,
			searchParams.isMajor,
			searchParams.type,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.presence,
			searchParams.at,
			limit,
		).map(mapper::toModel)
	}

	override fun findPlannedOnOwnDate(
		projectId: UUID,
		type: MovementTypeEnum,
		day: LocalDate,
		limit: Int,
	): Flux<ParticipantModel> {
		return repository.findIdsPlannedOnOwnDate(projectId, true, type == IN, day, limit)
			.collectList()
			.flatMapMany { ids -> findAllByIds(projectId, ids, true).sortedLike(ids) }
	}

	override fun findPlannedOnGroupDate(
		projectId: UUID,
		type: MovementTypeEnum,
		day: LocalDate,
		limit: Int,
	): Flux<Pair<UUID, ParticipantModel>> {
		return repository.findGroupContentsPlannedOnGroupDate(projectId, true, type == IN, day, limit)
			.collectList()
			.flatMapMany { contents -> withParticipants(projectId, contents) }
	}

	private fun withParticipants(
		projectId: UUID,
		contents: List<GroupContentEntity>,
	): Flux<Pair<UUID, ParticipantModel>> {
		val groupIdByParticipantId = contents.associate { it.participantId!! to it.groupId!! }
		return findAllByIds(projectId, groupIdByParticipantId.keys.toList(), true)
			.map { Pair(groupIdByParticipantId.getValue(it.id!!), it) }
	}

	private fun Flux<ParticipantModel>.sortedLike(ids: List<UUID>): Flux<ParticipantModel> {
		return collectList().flatMapMany { Flux.fromIterable(it.sortedBy { participant -> ids.indexOf(participant.id) }) }
	}

	override fun refreshEffectiveDates(ids: List<UUID>): Mono<Unit> {
		return if (ids.isEmpty()) Mono.just(Unit)
		else repository.refreshEffectiveDates(ids).thenReturn(Unit)
	}

	override fun lockByIds(ids: List<UUID>): Flux<UUID> {
		return if (ids.isEmpty()) Flux.empty() else repository.lockByIds(ids)
	}

	override fun saveAllGuest(guests: List<ParticipantModel>): Flux<ParticipantModel> {
		return if (guests.isEmpty()) Flux.empty()
		else repository.saveAll(guests.map(mapper::toEntity)).map(mapper::toModel)
	}

	override fun deleteAll(ids: List<UUID>): Mono<Unit> {
		return if (ids.isEmpty()) Mono.empty()
		else repository.deleteAllById(ids).thenReturn(Unit)
	}

	override fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUnusedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ParticipantModel> {
		return repository.findById(projectId, id, isVisible, at = null).map(mapper::toModel)
	}

	override fun create(element: ParticipantModel): Mono<ParticipantModel> {
		return save(element)
			.saveNewGroups(element)
			.reload()
			.`as`(transactionalOperator::transactional)
	}

	override fun update(element: ParticipantModel): Mono<ParticipantModel> {
		return save(element)
			.flatMap { findById(element.projectId!!, element.id!!, isVisible = null) }
			.saveNewGroups(element)
			.removeDeletedGroups(element)
			.reload()
			.`as`(transactionalOperator::transactional)
	}

	private fun Mono<ParticipantModel>.reload(): Mono<ParticipantModel> {
		return flatMap { findById(it.projectId!!, it.id!!, isVisible = null) }
	}

	fun Mono<ParticipantModel>.saveNewGroups(element: ParticipantModel): Mono<ParticipantModel> {
		return flatMap { participant ->
			val newGroups = participant.getNewGroups(element)
			if (newGroups.isEmpty()) return@flatMap Mono.just(participant)
			groupContentRepository.saveAll(newGroups.map { groupContentMapper.toEntity(it.id!!, participant) })
				.map(groupContentMapper::toModel)
				.collectList()
				.map { participant.apply { groups = groups.plus(newGroups) } }
		}
	}

	fun Mono<ParticipantModel>.removeDeletedGroups(element: ParticipantModel): Mono<ParticipantModel> {
		return flatMap { participant ->
			val removedGroups = participant.getOldGroupIds(element)
			if (removedGroups.isEmpty()) return@flatMap Mono.just(participant)
			groupContentRepository.deleteAllByParticipantIdAndGroupIds(participant.id!!, removedGroups)
				.then(Mono.fromCallable {
					participant.apply {
						groups = groups.filter { removedGroups.contains(it.id) }
					}
				})
		}
	}

	private fun save(element: ParticipantModel): Mono<ParticipantModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
