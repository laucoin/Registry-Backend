package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_STATUS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_TITLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertQueries.ALERT_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertQueries.ALERT_STATUS_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertQueries.ALERT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertQueries.SELECT_ALERT_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.AlertEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IAlertEntityRepository
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
class AlertModelPostgresRepository(
	private val repository: IAlertEntityRepository,
	private val mapper: AlertEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
) : IAlertPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: AlertSearchParamModel,
		sort: List<SortModel<AlertSortFieldEnum>>,
	): Mono<PageModel<AlertModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.statusSearched,
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
				searchParams.statusSearched,
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
	 * the [AlertSortFieldEnum] whitelist ([toColumn]) — user input never
	 * reaches the SQL string. Row mapping reuses the same converter Spring Data
	 * applies to the annotated queries.
	 */
	private fun findAllSorted(
		projectId: UUID,
		searchParams: AlertSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<AlertSortFieldEnum>>,
	): Flux<AlertEntity> {
		val orderBy =
			sort.joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" } + ", t.$ID ASC"
		val sql = """
        SELECT t.*, $SELECT_ALERT_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ALERT_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $ALERT_TEXT_SEARCH_CLAUSE AND $ALERT_STATUS_SEARCH_CLAUSE AND $ALERT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY $orderBy
        LIMIT :limit OFFSET :offset
        """

		var spec = databaseClient.sql(sql)
			.bind("projectId", projectId)
			.bind("statusSearched", searchParams.statusSearched.map { it.name })
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
			.map { row, metadata -> converter.read(AlertEntity::class.java, row, metadata) }
			.all()
	}

	private fun AlertSortFieldEnum.toColumn(): String = when (this) {
		AlertSortFieldEnum.DATE_TIME -> ALERT_DATE_TIME
		AlertSortFieldEnum.TITLE -> ALERT_TITLE
		AlertSortFieldEnum.STATUS -> ALERT_STATUS
	}

	override fun findWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: AlertSearchParamModel
	): Flux<AlertModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.statusSearched,
			searchParams.visibilitySearched,
			searchParams.startDateTimeSearched,
			searchParams.endDateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findOlderThanAndUncommentedSince(dateThreshold)
	}

	override fun findById(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?
	): Mono<AlertModel> {
		return repository.findById(
			projectId,
			id,
			visibilitySearched,
		).map(mapper::toModel)
	}

	override fun create(element: AlertModel): Mono<AlertModel> {
		return save(element)
	}

	override fun update(element: AlertModel): Mono<AlertModel> {
		return save(element)
	}

	private fun save(element: AlertModel): Mono<AlertModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
