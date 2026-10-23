package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IParticipantEntityRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
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
			searchParams.isPresent,
			searchParams.at,
			searchParams.hasUser,
			searchParams.birthdayFrom,
			searchParams.birthdayTo,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, ParticipantEntity::fullCount, mapper::toModel)
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
			searchParams.isPresent,
			searchParams.at,
			searchParams.hasUser,
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
			searchParams.isPresent,
			searchParams.at,
			searchParams.hasUser,
			searchParams.birthdayFrom,
			searchParams.birthdayTo,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, ParticipantEntity::fullCount, mapper::toModel)
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
			searchParams.isPresent,
			searchParams.at,
			limit,
		).map(mapper::toModel)
	}

	override fun updateAllEndAvailability(
		ids: List<UUID>,
		endAvailability: CustomDateTimeModel
	): Flux<ParticipantModel> {
		return if (ids.isEmpty()) Flux.empty()
		else repository.updateAllEndAvailability(ids, endAvailability.time, endAvailability.date).map(mapper::toModel)
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
			.`as`(transactionalOperator::transactional)
	}

	override fun update(element: ParticipantModel): Mono<ParticipantModel> {
		return save(element)
			.flatMap { findById(element.projectId!!, element.id!!, isVisible = null) }
			.saveNewGroups(element)
			.removeDeletedGroups(element)
			.`as`(transactionalOperator::transactional)
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
