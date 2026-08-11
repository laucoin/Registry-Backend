package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_LICENSE_PLATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleQueries.DATE_IN_VEHICLE_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleQueries.LAST_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleQueries.SELECT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleQueries.SELECT_VEHICLE_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleQueries.VEHICLE_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleQueries.VEHICLE_PRESENCE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleQueries.VEHICLE_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleQueries.WITH_VEHICLE_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.VehicleEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.orderByWithRelevance
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IVehicleEntityRepository
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
class VehicleModelPostgresRepository(
	private val repository: IVehicleEntityRepository,
	private val mapper: VehicleEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
) : IVehiclePort {
	override fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: VehicleSearchParamModel,
		sort: List<SortModel<VehicleSortFieldEnum>>,
	): Mono<PageModel<VehicleModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				projectId,
				searchParams.textSearched,
				searchParams.visibilitySearched,
				searchParams.availabilitySearched,
				searchParams.presenceSearched,
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
				searchParams.presenceSearched,
				searchParams.dateTimeSearched,
			),
			entities.map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	/**
	 * API v2 sorted page (ADR 017 §5). The ORDER BY is built exclusively from
	 * the [VehicleSortFieldEnum] whitelist ([toColumn]) — user input never
	 * reaches the SQL string. Row mapping reuses the same converter Spring Data
	 * applies to the annotated queries.
	 */
	private fun findAllSorted(
		projectId: UUID,
		searchParams: VehicleSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<VehicleSortFieldEnum>>,
	): Flux<VehicleEntity> {
		val orderBy = orderByWithRelevance(
			searchParams.textSearched,
			sort.joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" },
		)
		val sql = """
        WITH $WITH_VEHICLE_LAST_MOVEMENT
        SELECT t.*, $SELECT_LAST_MOVEMENT, $SELECT_VEHICLE_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $VEHICLE_TABLE t $LAST_MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VEHICLE_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $VEHICLE_AVAILABILITY_CLAUSE AND $VEHICLE_PRESENCE_CLAUSE AND $DATE_IN_VEHICLE_DATES_RANGE_CLAUSE
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
		spec = searchParams.presenceSearched
			?.let { spec.bind("presenceSearched", it) }
			?: spec.bindNull("presenceSearched", Boolean::class.javaObjectType)
		spec = searchParams.dateTimeSearched
			?.let { spec.bind("dateTimeSearched", it) }
			?: spec.bindNull("dateTimeSearched", ZonedDateTime::class.java)

		return spec
			.map { row, metadata -> converter.read(VehicleEntity::class.java, row, metadata) }
			.all()
	}

	private fun VehicleSortFieldEnum.toColumn(): String = when (this) {
		VehicleSortFieldEnum.LICENSE_PLATE -> VEHICLE_LICENSE_PLATE
		VehicleSortFieldEnum.BRAND -> VEHICLE_BRAND
		VehicleSortFieldEnum.MODEL -> VEHICLE_MODEL
	}

	override fun countAll(
		projectId: UUID,
		searchParams: VehicleSearchParamModel
	): Mono<Long> {
		return repository.countAll(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
		)
	}

	override fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<VehicleModel> {
		return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, visibilitySearched)
			.map(mapper::toModel)
	}

	override fun findWithLimit(limit: Int, projectId: UUID, searchParams: VehicleSearchParamModel): Flux<VehicleModel> {
		return repository.findWithLimit(
			projectId,
			searchParams.textSearched,
			searchParams.visibilitySearched,
			searchParams.availabilitySearched,
			searchParams.presenceSearched,
			searchParams.dateTimeSearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUnusedSince(dateThreshold)
	}

	override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<VehicleModel> {
		return repository.findById(projectId, id, visibilitySearched).map(mapper::toModel)
	}

	override fun create(element: VehicleModel): Mono<VehicleModel> {
		return save(element)
	}

	override fun update(element: VehicleModel): Mono<VehicleModel> {
		return save(element)
	}

	private fun save(element: VehicleModel): Mono<VehicleModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
