package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.model.OpenAlertProjectModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectQueries.DATE_IN_PROJECT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectQueries.PROJECT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.OpenAlertProjectEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.ProjectEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IProjectEntityRepository
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
class ProjectModelPostgresRepository(
	private val repository: IProjectEntityRepository,
	private val mapper: ProjectEntityMapper,
	private val openAlertMapper: OpenAlertProjectEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
) : IProjectPort {

	override fun findOpenAlertProjectsByUserId(userId: UUID, limit: Int): Flux<OpenAlertProjectModel> {
		return repository.findOpenAlertProjectsByUserId(userId, limit).map(openAlertMapper::toModel)
	}

	override fun findPage(
		pageable: PageableModel,
		searchParams: ProjectSearchParamModel,
		sort: List<SortModel<ProjectSortFieldEnum>>,
	): Mono<PageModel<ProjectModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.dateTimeSearched,
				pageable.limit,
				pageable.offset,
			)
		} else {
			findAllSorted(searchParams, pageable, sort)
		}

		return Mono.zip(
			repository.countAll(
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.dateTimeSearched,
			),
			entities.map(mapper::toModel).collectList(),
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	override fun findPage(
		projectIds: List<UUID>,
		pageable: PageableModel,
		searchParams: ProjectSearchParamModel,
		sort: List<SortModel<ProjectSortFieldEnum>>,
	): Mono<PageModel<ProjectModel>> {
		if (projectIds.isEmpty()) {
			return Mono.just(PageModel(pageable, 0, emptyList()))
		}

		val entities = if (sort.isEmpty()) {
			repository.findAllInProjectIds(
				projectIds,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.dateTimeSearched,
				pageable.limit,
				pageable.offset,
			)
		} else {
			findAllSorted(searchParams, pageable, sort, projectIds)
		}

		return Mono.zip(
			repository.countAllInProjectIds(
				projectIds,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.dateTimeSearched,
			),
			entities.map(mapper::toModel).collectList(),
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	/**
	 * API v2 sorted page (ADR 017 §5). The ORDER BY is built exclusively from
	 * the [ProjectSortFieldEnum] whitelist ([toColumn]) — user input never
	 * reaches the SQL string. Row mapping reuses the same converter Spring Data
	 * applies to the annotated queries.
	 */
	private fun findAllSorted(
		searchParams: ProjectSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<ProjectSortFieldEnum>>,
		projectIds: List<UUID>? = null,
	): Flux<ProjectEntity> {
		val orderBy =
			sort.joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" } + ", t.$ID ASC"
		val projectIdsClause = if (projectIds != null) "t.$ID IN (:projectIds) AND " else ""
		val sql = """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $PROJECT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $projectIdsClause$PROJECT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_PROJECT_DATES_RANGE_CLAUSE
        ORDER BY $orderBy
        LIMIT :limit OFFSET :offset
        """

		var spec = databaseClient.sql(sql)
			.bind("limit", pageable.limit)
			.bind("offset", pageable.offset)
		projectIds?.let { spec = spec.bind("projectIds", it) }
		spec = searchParams.textSearched
			?.let { spec.bind("textSearched", it) }
			?: spec.bindNull("textSearched", String::class.java)
		spec = searchParams.visibilitySearched
			?.let { spec.bind("visibilitySearched", it) }
			?: spec.bindNull("visibilitySearched", Boolean::class.javaObjectType)
		spec = searchParams.dateTimeSearched
			?.let { spec.bind("dateTimeSearched", it) }
			?: spec.bindNull("dateTimeSearched", ZonedDateTime::class.java)

		return spec
			.map { row, metadata -> converter.read(ProjectEntity::class.java, row, metadata) }
			.all()
	}

	private fun ProjectSortFieldEnum.toColumn(): String = when (this) {
		ProjectSortFieldEnum.NAME -> PROJECT_NAME
		ProjectSortFieldEnum.BEGIN_DATE -> PROJECT_BEGIN_DATE
		ProjectSortFieldEnum.END_DATE -> PROJECT_END_DATE
	}

	override fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<Boolean> {
		return repository.validDateTime(id, begin, end)
			.map { (it.count ?: 0) == 0 }
	}

	override fun findProjectsEligibleForPurge(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findProjectsEligibleForPurge(dateThreshold)
	}

	override fun findById(id: UUID, visibilitySearched: Boolean?): Mono<ProjectModel> {
		return repository.findById(id, visibilitySearched)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun create(element: ProjectModel): Mono<ProjectModel> {
		return save(element)
	}

	override fun update(element: ProjectModel): Mono<ProjectModel> {
		return save(element)
	}

	private fun save(element: ProjectModel): Mono<ProjectModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
