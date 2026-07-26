package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.CommunicationSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationFields.COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationFields.COMMUNICATION_MESSAGE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationQueries.ALERT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationQueries.COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationQueries.COMMUNICATION_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationQueries.MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationQueries.SELECT_COMMUNICATION_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationQueries.SELECT_LINKED_ALERT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationQueries.SELECT_LINKED_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.CommunicationEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.ICommunicationEntityRepository
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Service
class CommunicationPostgresRepository(
	private val repository: ICommunicationEntityRepository,
	private val mapper: CommunicationEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
) : ICommunicationPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
		sort: List<SortModel<CommunicationSortFieldEnum>>,
	): Mono<PageModel<CommunicationModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
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
				searchParams.textSearched,
				searchParams.visibilitySearched,
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
	 * the [CommunicationSortFieldEnum] whitelist ([toColumn]) — user input never
	 * reaches the SQL string. Row mapping reuses the same converter Spring Data
	 * applies to the annotated queries.
	 */
	private fun findAllSorted(
		projectId: UUID,
		searchParams: CommunicationSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<CommunicationSortFieldEnum>>,
	): Flux<CommunicationEntity> {
		val orderBy =
			sort.joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" } + ", t.$ID ASC"
		val sql = """
        SELECT t.*, $SELECT_COMMUNICATION_SEARCH, $SELECT_LINKED_MOVEMENT, $SELECT_LINKED_ALERT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $COMMUNICATION_TABLE t $MOVEMENT_JOIN $ALERT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $COMMUNICATION_TEXT_SEARCH_CLAUSE AND $COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY $orderBy
        LIMIT :limit OFFSET :offset
        """

		var spec = databaseClient.sql(sql)
			.bind("projectId", projectId)
			.bind("limit", pageable.limit)
			.bind("offset", pageable.offset)
		spec = searchParams.textSearched
			?.let { spec.bind("textSearched", it) }
			?: spec.bindNull("textSearched", String::class.java)
		spec = searchParams.visibilitySearched
			?.let { spec.bind("visibilitySearched", it) }
			?: spec.bindNull("visibilitySearched", Boolean::class.javaObjectType)
		spec = searchParams.startDateTimeSearched
			?.let { spec.bind("startDateTimeSearched", it) }
			?: spec.bindNull("startDateTimeSearched", ZonedDateTime::class.java)
		spec = searchParams.endDateTimeSearched
			?.let { spec.bind("endDateTimeSearched", it) }
			?: spec.bindNull("endDateTimeSearched", ZonedDateTime::class.java)

		return spec
			.map { row, metadata -> converter.read(CommunicationEntity::class.java, row, metadata) }
			.all()
	}

	private fun CommunicationSortFieldEnum.toColumn(): String = when (this) {
		CommunicationSortFieldEnum.DATE_TIME -> COMMUNICATION_DATE_TIME
		CommunicationSortFieldEnum.MESSAGE -> COMMUNICATION_MESSAGE
	}

	override fun findByMovementIdsWithLimit(
		limit: Int,
		projectId: UUID,
		movementIds: List<UUID>,
		visibilitySearched: Boolean?,
	): Flux<Pair<UUID, List<CommunicationModel>>> {
		return if (movementIds.isEmpty()) Flux.empty()
		else repository.findAllByMovementIdsWithLimit(projectId, movementIds, visibilitySearched, limit)
			.groupBy { it.movementId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(mapper::toModel) }
			}
	}

	override fun findPageByMovementId(
		projectId: UUID,
		movementId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>> {
		return Mono.zip(
			countAllByMovementId(projectId, movementId, searchParams),
			repository.findAllByMovementId(
				projectId,
				movementId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findByAlertIdsWithLimit(
		limit: Int,
		projectId: UUID,
		alertIds: List<UUID>,
		visibilitySearched: Boolean?
	): Flux<Pair<UUID, List<CommunicationModel>>> {
		return if (alertIds.isEmpty()) Flux.empty()
		else repository.findAllByAlertIdsWithLimit(projectId, alertIds, visibilitySearched, limit)
			.groupBy { it.movementId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(mapper::toModel) }
			}
	}

	override fun findPageByAlertId(
		projectId: UUID,
		alertId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>> {
		return Mono.zip(
			countAllByAlertId(projectId, alertId, searchParams),
			repository.findAllByAlertId(
				projectId,
				alertId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.startDateTimeSearched,
				searchParams.endDateTimeSearched,
				pageable.limit,
				pageable.offset,
			).map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findAllByIds(
		projectId: UUID,
		ids: List<UUID>,
		visibilitySearched: Boolean?
	): Flux<CommunicationModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, visibilitySearched)
			.map(mapper::toModel)
	}

	override fun countAllByMovementId(
		projectId: UUID,
		movementId: UUID,
		searchParams: CommunicationSearchParamModel
	): Mono<Long> {
		return repository.countAllByMovementId(
			projectId,
			movementId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
		)
	}

	override fun countAllByAlertId(
		projectId: UUID,
		alertId: UUID,
		searchParams: CommunicationSearchParamModel
	): Mono<Long> {
		return repository.countAllByAlertId(
			projectId,
			alertId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
		)
	}

	override fun findOrphan(
		movementsToExclude: List<UUID>,
		alertsToExclude: List<UUID>,
	): Flux<UUID> {
		return when {
			movementsToExclude.isEmpty() && alertsToExclude.isEmpty() -> repository.findOrphan()
			alertsToExclude.isEmpty() -> repository.findOrphanExcludingMovements(movementsToExclude)
			movementsToExclude.isEmpty() -> repository.findOrphanExcludingAlerts(alertsToExclude)
			else -> repository.findOrphanExcludingMovementsAndAlerts(movementsToExclude, alertsToExclude)
		}
	}

	override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<CommunicationModel> {
		return repository.findById(projectId, id, visibilitySearched)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun create(element: CommunicationModel): Mono<CommunicationModel> {
		return save(element)
	}

	override fun update(element: CommunicationModel): Mono<CommunicationModel> {
		return save(element)
	}

	private fun save(element: CommunicationModel): Mono<CommunicationModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
