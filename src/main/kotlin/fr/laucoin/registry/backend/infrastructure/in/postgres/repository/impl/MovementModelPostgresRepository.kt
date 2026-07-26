package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementSortFieldEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_REASON
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.ACTIVITY_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.CURRENT_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.MOVEMENT_ACTIVITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.MOVEMENT_TYPE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.SELECT_LINKED_ACTIVITY
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.WITH_CURRENT_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IMovementContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IMovementEntityRepository
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MovementModelPostgresRepository(
	private val repository: IMovementEntityRepository,
	private val contentRepository: IMovementContentEntityRepository,
	private val transactionalOperator: TransactionalOperator,
	private val mapper: MovementEntityMapper,
	private val contentMapper: MovementContentEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
) : IMovementPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
		sort: List<SortModel<MovementSortFieldEnum>>,
	): Mono<PageModel<MovementModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				projectId,
				searchParams.visibilitySearched,
				searchParams.linkedToActivity,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
				pageable.limit,
				pageable.offset,
			)
		} else {
			findAllSorted(projectId, searchParams, pageable, sort)
		}

		return Mono.zip(
			repository.countAll(
				projectId,
				searchParams.visibilitySearched,
				searchParams.linkedToActivity,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
			),
			entities.map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findCurrentPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
		sort: List<SortModel<MovementSortFieldEnum>>,
	): Mono<PageModel<MovementModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findCurrent(
				projectId,
				searchParams.visibilitySearched,
				searchParams.linkedToActivity,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
				pageable.limit,
				pageable.offset,
			)
		} else {
			findCurrentSorted(projectId, searchParams, pageable, sort)
		}

		return Mono.zip(
			repository.countCurrent(
				projectId,
				searchParams.visibilitySearched,
				searchParams.linkedToActivity,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
			),
			entities.map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	/**
	 * API v2 sorted page (ADR 017 §5). The ORDER BY is built exclusively from
	 * the [MovementSortFieldEnum] whitelist ([toColumn]) — user input never
	 * reaches the SQL string. Row mapping reuses the same converter Spring Data
	 * applies to the annotated queries.
	 */
	private fun findAllSorted(
		projectId: UUID,
		searchParams: MovementSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<MovementSortFieldEnum>>,
	): Flux<MovementEntity> {
		val sql = """
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY ${sort.toOrderBy()}
        LIMIT :limit OFFSET :offset
        """

		return bindAndRead(sql, projectId, searchParams, pageable)
	}

	private fun findCurrentSorted(
		projectId: UUID,
		searchParams: MovementSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<MovementSortFieldEnum>>,
	): Flux<MovementEntity> {
		val sql = """
        WITH $WITH_CURRENT_MOVEMENT
        SELECT DISTINCT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $CURRENT_MOVEMENT_JOIN $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY ${sort.toOrderBy()}
        LIMIT :limit OFFSET :offset
        """

		return bindAndRead(sql, projectId, searchParams, pageable)
	}

	private fun List<SortModel<MovementSortFieldEnum>>.toOrderBy(): String =
		joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" } + ", t.$ID ASC"

	private fun bindAndRead(
		sql: String,
		projectId: UUID,
		searchParams: MovementSearchParamModel,
		pageable: PageableModel,
	): Flux<MovementEntity> {
		var spec = databaseClient.sql(sql)
			.bind("projectId", projectId)
			.bind("typeSearched", searchParams.typeSearched.map { it.name })
			.bind("limit", pageable.limit)
			.bind("offset", pageable.offset)
		spec = searchParams.visibilitySearched
			?.let { spec.bind("visibilitySearched", it) }
			?: spec.bindNull("visibilitySearched", Boolean::class.javaObjectType)
		spec = searchParams.linkedToActivity
			?.let { spec.bind("linkedToActivity", it) }
			?: spec.bindNull("linkedToActivity", Boolean::class.javaObjectType)
		spec = searchParams.startDateTimeSearched
			?.let { spec.bind("startDateTimeSearched", it.toOffsetDateTime()) }
			?: spec.bindNull("startDateTimeSearched", OffsetDateTime::class.java)
		spec = searchParams.endDateTimeSearched
			?.let { spec.bind("endDateTimeSearched", it.toOffsetDateTime()) }
			?: spec.bindNull("endDateTimeSearched", OffsetDateTime::class.java)

		return spec
			.map { row, metadata -> converter.read(MovementEntity::class.java, row, metadata) }
			.all()
	}

	private fun MovementSortFieldEnum.toColumn(): String = when (this) {
		MovementSortFieldEnum.DATE_TIME -> MOVEMENT_DATE_TIME
		MovementSortFieldEnum.TYPE -> MOVEMENT_TYPE
		MovementSortFieldEnum.REASON -> MOVEMENT_REASON
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
		return Mono.zip(
			repository.countAllByParticipantId(
				projectId,
				participantId,
				searchParams.visibilitySearched,
				searchParams.linkedToActivity,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
			),
			repository.findAllByParticipantId(
				projectId,
				participantId,
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

	override fun findPageByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return Mono.zip(
			repository.countAllByVehicleId(
				projectId,
				vehicleId,
				searchParams.visibilitySearched,
				searchParams.linkedToActivity,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
			),
			repository.findAllByVehicleId(
				projectId,
				vehicleId,
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

	override fun findPageByActivityId(
		projectId: UUID,
		activityId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return Mono.zip(
			repository.countAllByActivityId(
				projectId,
				activityId,
				searchParams.visibilitySearched,
				searchParams.typeSearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
			),
			repository.findAllByActivityId(
				projectId,
				activityId,
				searchParams.visibilitySearched,
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

	override fun findOngoingActivities(projectId: UUID, limit: Int): Flux<MovementModel> {
		return repository.findOngoingActivities(projectId, visibilitySearched = true, limit).map(mapper::toModel)
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
					if (it.isEmpty()) handle.next(emptyList()) else handle.next(
						it.first().second
					)
				}
		).map {
			it.t1.content = it.t2
			it.t1
		}
	}

	override fun create(element: MovementModel): Mono<MovementModel> {
		return save(element)
			.saveNewContent(element)
			.`as`(transactionalOperator::transactional)
	}

	override fun update(element: MovementModel): Mono<MovementModel> {
		return save(element)
			.flatMap { findById(element.project!!.id!!, element.id!!, visibilitySearched = null) }
			.removeDeletedContent(element)
			.saveNewContent(element)
			.`as`(transactionalOperator::transactional)
	}

	fun Mono<MovementModel>.saveNewContent(element: MovementModel): Mono<MovementModel> {
		return flatMap { movement ->
			val newContent = movement.getNewContent(element)
			if (newContent.isEmpty()) return@flatMap Mono.just(movement)
			contentRepository.saveAll(newContent.map { contentMapper.toEntity(movement.id!!, it) })
				.map(contentMapper::toModel)
				.collectList()
				.map { movement.apply { content = content.plus(it) } }
		}
	}

	fun Mono<MovementModel>.removeDeletedContent(element: MovementModel): Mono<MovementModel> {
		return flatMap { movement ->
			val removedIds = movement.getOldContentIds(element)
			if (removedIds.isEmpty()) return@flatMap Mono.just(movement)
			contentRepository.deleteAllById(removedIds)
				.then(Mono.fromCallable { movement.apply { content = content.filter { !removedIds.contains(it.id) } } })
		}
	}

	private fun save(element: MovementModel): Mono<MovementModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
