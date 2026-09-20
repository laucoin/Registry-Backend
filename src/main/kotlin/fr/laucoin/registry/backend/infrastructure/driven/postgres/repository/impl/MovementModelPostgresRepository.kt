package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.MovementContentJooqRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.MovementJooqRepository
import java.time.LocalDate
import java.util.UUID
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class MovementModelPostgresRepository(
	private val repository: MovementJooqRepository,
	private val contentRepository: MovementContentJooqRepository,
	private val dsl: DSLContext,
	private val mapper: MovementEntityMapper,
	private val contentMapper: MovementContentEntityMapper,
): IMovementPort {
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
				.handle<List<MovementContentModel>> { it, handle -> if (it.isNullOrEmpty()) handle.next(emptyList()) else handle.next(it.first().second) }
		).map {
			it.t1.content = it.t2
			it.t1
		}
	}

	override fun create(element: MovementModel): Mono<MovementModel> = Mono.from(
		dsl.transactionPublisher { config ->
			val txDsl = config.dsl()
			save(txDsl, element).saveNewContent(txDsl, element)
		}
	)

	override fun update(element: MovementModel): Mono<MovementModel> = Mono.from(
		dsl.transactionPublisher { config ->
			val txDsl = config.dsl()
			save(txDsl, element)
				.flatMap { findById(element.project!!.id!!, element.id!!, visibilitySearched = null) }
				.removeDeletedContent(txDsl, element)
				.saveNewContent(txDsl, element)
		}
	)

	fun Mono<MovementModel>.saveNewContent(txDsl: DSLContext, element: MovementModel): Mono<MovementModel> {
		return flatMap { movement ->
			val newContent = movement.getNewContent(element)
			if (newContent.isEmpty()) return@flatMap Mono.just(movement)
			contentRepository.saveAll(newContent.map { contentMapper.toEntity(movement.id!!, it) }, txDsl)
				.map(contentMapper::toModel)
				.collectList()
				.map { movement.apply { content = content.plus(it) } }
		}
	}

	fun Mono<MovementModel>.removeDeletedContent(txDsl: DSLContext, element: MovementModel): Mono<MovementModel> {
		return flatMap { movement ->
			val removedIds = movement.getOldContentIds(element)
			if (removedIds.isEmpty()) return@flatMap Mono.just(movement)
			contentRepository.deleteAllById(removedIds, txDsl)
				.then(Mono.fromCallable { movement.apply { content = content.filter { !removedIds.contains(it.id) } } })
		}
	}

	private fun save(txDsl: DSLContext, element: MovementModel): Mono<MovementModel> {
		return repository.save(mapper.toEntity(element), txDsl).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
