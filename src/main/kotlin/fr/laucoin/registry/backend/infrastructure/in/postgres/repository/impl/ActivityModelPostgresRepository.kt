package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityQueries.ACTIVITY_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityQueries.ACTIVITY_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityQueries.DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityQueries.SELECT_ACTIVITY_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.orderByWithRelevance
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IActivityEntityRepository
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
class ActivityModelPostgresRepository(
	private val repository: IActivityEntityRepository,
	private val mapper: ActivityEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
) : IActivityPort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
		sort: List<SortModel<ActivitySortFieldEnum>>,
	): Mono<PageModel<ActivityModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.dateTimeSearched,
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
				searchParams.availabilitySearched,
				searchParams.dateTimeSearched,
			),
			entities.map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	/**
	 * API v2 sorted page. The ORDER BY is built exclusively from
	 * the [ActivitySortFieldEnum] whitelist ([toColumn]) — user input never
	 * reaches the SQL string. Row mapping reuses the same converter Spring Data
	 * applies to the annotated queries.
	 */
	private fun findAllSorted(
		projectId: UUID,
		searchParams: ActivitySearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<ActivitySortFieldEnum>>,
	): Flux<ActivityEntity> {
		val orderBy = orderByWithRelevance(
			searchParams.textSearched,
			sort.joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" },
		)
		val sql = """
        SELECT t.*, $SELECT_ACTIVITY_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $ACTIVITY_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $ACTIVITY_AVAILABILITY_CLAUSE AND $DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
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
		spec = searchParams.availabilitySearched
			?.let { spec.bind("availabilitySearched", it) }
			?: spec.bindNull("availabilitySearched", Boolean::class.javaObjectType)
		spec = searchParams.dateTimeSearched
			?.let { spec.bind("dateTimeSearched", it) }
			?: spec.bindNull("dateTimeSearched", ZonedDateTime::class.java)

		return spec
			.map { row, metadata -> converter.read(ActivityEntity::class.java, row, metadata) }
			.all()
	}

	private fun ActivitySortFieldEnum.toColumn(): String = when (this) {
		ActivitySortFieldEnum.NAME -> ACTIVITY_NAME
		ActivitySortFieldEnum.DURATION -> ACTIVITY_DURATION
		ActivitySortFieldEnum.START_AVAILABILITY_DATE -> ACTIVITY_START_AVAILABILITY_DATE
		ActivitySortFieldEnum.END_AVAILABILITY_DATE -> ACTIVITY_END_AVAILABILITY_DATE
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, visibilitySearched)
			.map(mapper::toModel)
	}

	override fun findWithLimit(
		limit: Int,
		projectId: UUID,
		searchParams: ActivitySearchParamModel
	): Flux<ActivityModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.dateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUnusedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityModel> {
		return repository.findById(projectId, id, visibilitySearched)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun create(element: ActivityModel): Mono<ActivityModel> {
		return save(element)
	}

	override fun update(element: ActivityModel): Mono<ActivityModel> {
		return save(element)
	}

	private fun save(element: ActivityModel): Mono<ActivityModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
