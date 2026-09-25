package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.MovementContentJooqRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.MovementJooqRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Service
class MovementModelPostgresRepository(
	private val repository: MovementJooqRepository,
	private val contentRepository: MovementContentJooqRepository,
	private val dsl: DSLContext,
	private val mapper: MovementEntityMapper,
	private val contentMapper: MovementContentEntityMapper,
) : IMovementPort {
	override fun findAllByCreatorId(userId: UUID): Flux<MovementModel> {
		return repository.findAllByCreatorId(userId).map(mapper::toModel)
	}

	override fun findOngoingActivityOutings(projectId: UUID, limit: Int): Flux<MovementModel> {
		return repository.findOngoingActivityOutings(projectId, limit).map(mapper::toModel)
	}

	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return repository.findAll(
			projectId,
			searchParams.visibilitySearched,
			searchParams.linkedToActivity,
			searchParams.typeSearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, MovementEntity::fullCount, mapper::toModel)
	}

	override fun findCurrentPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return Mono.zip(
			repository.countCurrent(
				projectId,
				searchParams.visibilitySearched,
				searchParams.linkedToActivity,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
			),
			repository.findCurrent(
				projectId,
				searchParams.visibilitySearched,
				searchParams.linkedToActivity,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findContent(projectId: UUID, movementIds: List<UUID>): Flux<Pair<UUID, List<MovementContentModel>>> {
		return if (movementIds.isEmpty()) Flux.empty()
		else contentRepository.findAllByMovementIds(projectId, movementIds)
			.groupBy { it.movementId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(contentMapper::toModel) }
			}
	}

	override fun findCurrentContent(
		projectId: UUID,
		movementIds: List<UUID>
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return if (movementIds.isEmpty()) Flux.empty()
		else contentRepository.findCurrentByMovementIds(projectId, movementIds)
			.groupBy { it.movementId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(contentMapper::toModel) }
			}
	}

	override fun findPageByParticipantId(
		projectId: UUID,
		participantId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return repository.findAllByParticipantId(
			projectId,
			participantId,
			searchParams.visibilitySearched,
			searchParams.linkedToActivity,
			searchParams.typeSearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, MovementEntity::fullCount, mapper::toModel)
	}

	override fun findPageByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return repository.findAllByVehicleId(
			projectId,
			vehicleId,
			searchParams.visibilitySearched,
			searchParams.linkedToActivity,
			searchParams.typeSearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, MovementEntity::fullCount, mapper::toModel)
	}

	override fun findPageByActivityId(
		projectId: UUID,
		activityId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return repository.findAllByActivityId(
			projectId,
			activityId,
			searchParams.visibilitySearched,
			searchParams.typeSearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, MovementEntity::fullCount, mapper::toModel)
	}

	override fun findActivityWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: ActivitySearchParamModel
	): Flux<MovementModel> {
		return repository.findByActivityWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.dateTimeSearched,
			limit,
		).map { mapper.toModel(it) }
	}

	override fun countAllByParticipantId(
		projectId: UUID,
		participantId: UUID,
		searchParams: MovementSearchParamModel,
	): Mono<Long> {
		return repository.countAllByParticipantId(
			projectId,
			participantId,
			searchParams.visibilitySearched,
			searchParams.linkedToActivity,
			searchParams.typeSearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
		)
	}

	override fun countAllByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		searchParams: MovementSearchParamModel,
	): Mono<Long> {
		return repository.countAllByVehicleId(
			projectId,
			vehicleId,
			searchParams.visibilitySearched,
			searchParams.linkedToActivity,
			searchParams.typeSearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
		)
	}

	override fun countAllByActivityId(
		projectId: UUID,
		activityId: UUID,
		searchParams: MovementSearchParamModel,
	): Mono<Long> {
		return repository.countAllByActivityId(
			projectId,
			activityId,
			searchParams.visibilitySearched,
			searchParams.typeSearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
		)
	}

	override fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findOlderThanAndUncommentedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementModel> {
		return Mono.zip(
			repository.findById(projectId, id, visibilitySearched).map(mapper::toModel),
			findContent(projectId, listOf(id)).collectList()
				.handle<List<MovementContentModel>> { it, handle ->
					if (it.isNullOrEmpty()) handle.next(emptyList()) else handle.next(
						it.first().second
					)
				}
		).map {
			it.t1.content = it.t2
			it.t1
		}
	}

	// Atomicity across the save + content diff is the caller's responsibility (`TransactionalOperator`),
	// not this repository's — see the jOOQ-vs-Spring-transaction note on `JooqConfig.dslContext()`.
	override fun create(element: MovementModel): Mono<MovementModel> {
		return save(dsl, element).saveNewContent(dsl, element)
	}

	override fun update(element: MovementModel): Mono<MovementModel> {
		return save(dsl, element)
			.flatMap { findById(dsl, element.project!!.id!!, element.id!!) }
			.removeDeletedContent(dsl, element)
			.saveNewContent(dsl, element)
	}

	private fun findById(using: DSLContext, projectId: UUID, id: UUID): Mono<MovementModel> {
		return Mono.zip(
			repository.findById(projectId, id, visibilitySearched = null, using = using).map(mapper::toModel),
			contentRepository.findAllByMovementIds(projectId, listOf(id), using = using).map(contentMapper::toModel)
				.collectList(),
		).map {
			it.t1.content = it.t2
			it.t1
		}
	}

	fun Mono<MovementModel>.saveNewContent(using: DSLContext, element: MovementModel): Mono<MovementModel> {
		return flatMap { movement ->
			val newContent = movement.getNewContent(element)
			if (newContent.isEmpty()) return@flatMap Mono.just(movement)
			contentRepository.saveAll(newContent.map { contentMapper.toEntity(movement.id!!, it) }, using)
				.map(contentMapper::toModel)
				.collectList()
				.map { movement.apply { content = content.plus(it) } }
		}
	}

	fun Mono<MovementModel>.removeDeletedContent(using: DSLContext, element: MovementModel): Mono<MovementModel> {
		return flatMap { movement ->
			val removedIds = movement.getOldContentIds(element)
			if (removedIds.isEmpty()) return@flatMap Mono.just(movement)
			contentRepository.deleteAllById(removedIds, using)
				.then(Mono.fromCallable { movement.apply { content = content.filter { !removedIds.contains(it.id) } } })
		}
	}

	private fun save(using: DSLContext, element: MovementModel): Mono<MovementModel> {
		return repository.save(mapper.toEntity(element), using).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
