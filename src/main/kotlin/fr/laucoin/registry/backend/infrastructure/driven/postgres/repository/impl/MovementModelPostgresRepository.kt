package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria.MovementCriteria
import fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria.PageQuery
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.MovementContentPostgresHelper
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import fr.laucoin.registry.backend.domain.model.MovementEventModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ActivityStatisticsEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementEventEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IMovementEntityRepository
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port du mouvement.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class MovementModelPostgresRepository(
	private val repository: IMovementEntityRepository,
	private val content: MovementContentPostgresHelper,
	private val transactionalOperator: TransactionalOperator,
	private val mapper: MovementEntityMapper,
	private val eventMapper: MovementEventEntityMapper,
	private val statisticsMapper: ActivityStatisticsEntityMapper,
	private val clock: Clock,
): IMovementPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return repository.findAll(
			projectId,
			searchParams.isVisible,
			searchParams.activityId,
			MovementCriteria(
				hasActivity = searchParams.hasActivity,
				type = searchParams.type,
				from = searchParams.from,
				to = searchParams.to,
			),
			PageQuery(
				sortField = searchParams.sort?.field,
				isDescending = searchParams.sort?.isDescending,
				limit = pageable.limit,
				offset = pageable.offset,
			),
		).toPageModel(pageable, clock, MovementEntity::fullCount, mapper::toModel)
	}

	override fun findCurrentPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return repository.findCurrent(
			projectId,
			searchParams.isVisible,
			searchParams.activityId,
			MovementCriteria(
				hasActivity = searchParams.hasActivity,
				type = searchParams.type,
				from = searchParams.from,
				to = searchParams.to,
			),
			PageQuery(
				sortField = searchParams.sort?.field,
				isDescending = searchParams.sort?.isDescending,
				limit = pageable.limit,
				offset = pageable.offset,
			),
		).toPageModel(pageable, clock, MovementEntity::fullCount, mapper::toModel)
	}

	override fun findContent(projectId: UUID, movementIds: List<UUID>): Flux<Pair<UUID, List<MovementContentModel>>> {
		return content.findContent(projectId, movementIds)
	}

	override fun findCurrentContent(
		projectId: UUID,
		movementIds: List<UUID>
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return content.findCurrentContent(projectId, movementIds)
	}

	override fun findEventsByParticipantId(projectId: UUID, participantId: UUID): Flux<MovementEventModel> {
		return repository.findEventsByParticipantId(projectId, participantId).map(eventMapper::toModel)
	}

	override fun findStatisticsByActivityId(projectId: UUID, activityId: UUID): Mono<ActivityStatisticsModel> {
		return repository.findStatisticsByActivityId(projectId, activityId).map(statisticsMapper::toModel)
	}

	override fun findEventsByVehicleId(projectId: UUID, vehicleId: UUID): Flux<MovementEventModel> {
		return repository.findEventsByVehicleId(projectId, vehicleId).map(eventMapper::toModel)
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
			searchParams.isVisible,
			MovementCriteria(
				hasActivity = searchParams.hasActivity,
				type = searchParams.type,
				from = searchParams.from,
				to = searchParams.to,
			),
			PageQuery(
				sortField = searchParams.sort?.field,
				isDescending = searchParams.sort?.isDescending,
				limit = pageable.limit,
				offset = pageable.offset,
			),
		).toPageModel(pageable, clock, MovementEntity::fullCount, mapper::toModel)
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
			searchParams.isVisible,
			MovementCriteria(
				hasActivity = searchParams.hasActivity,
				type = searchParams.type,
				from = searchParams.from,
				to = searchParams.to,
			),
			PageQuery(
				sortField = searchParams.sort?.field,
				isDescending = searchParams.sort?.isDescending,
				limit = pageable.limit,
				offset = pageable.offset,
			),
		).toPageModel(pageable, clock, MovementEntity::fullCount, mapper::toModel)
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
			searchParams.isVisible,
			MovementCriteria(
				type = searchParams.type,
				from = searchParams.from,
				to = searchParams.to,
			),
			PageQuery(
				sortField = searchParams.sort?.field,
				isDescending = searchParams.sort?.isDescending,
				limit = pageable.limit,
				offset = pageable.offset,
			),
		).toPageModel(pageable, clock, MovementEntity::fullCount, mapper::toModel)
	}

	override fun findActivityWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: ActivitySearchParamModel
	): Flux<MovementModel> {
		return repository.findByActivityWithLimit(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.at,
			PageQuery(limit = limit),
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
			searchParams.isVisible,
			MovementCriteria(
				hasActivity = searchParams.hasActivity,
				type = searchParams.type,
				from = searchParams.from,
				to = searchParams.to,
			),
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
			searchParams.isVisible,
			MovementCriteria(
				hasActivity = searchParams.hasActivity,
				type = searchParams.type,
				from = searchParams.from,
				to = searchParams.to,
			),
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
			searchParams.isVisible,
			MovementCriteria(
				type = searchParams.type,
				from = searchParams.from,
				to = searchParams.to,
			),
		)
	}

	override fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findOlderThanAndUncommentedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel> {
		return Mono.zip(
			repository.findById(projectId, id, isVisible).map(mapper::toModel),
			findContent(projectId, listOf(id)).collectList()
				.map { it.firstOrNull()?.second ?: emptyList() }
		).map { it.t1.apply { content = it.t2 } }
	}

	override fun create(element: MovementModel): Mono<MovementModel> {
		return save(element)
			.saveNewContent(element)
			.`as`(transactionalOperator::transactional)
	}

	override fun update(element: MovementModel): Mono<MovementModel> {
		return save(element)
			.flatMap { findById(element.projectId!!, element.id!!, isVisible = null) }
			.removeDeletedContent(element)
			.saveNewContent(element)
			.`as`(transactionalOperator::transactional)
	}

	private fun Mono<MovementModel>.saveNewContent(element: MovementModel): Mono<MovementModel> {
		return content.saveNewContent(this, element)
	}

	private fun Mono<MovementModel>.removeDeletedContent(element: MovementModel): Mono<MovementModel> {
		return content.removeDeletedContent(this, element)
	}

	private fun save(element: MovementModel): Mono<MovementModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
