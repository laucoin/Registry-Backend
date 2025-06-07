package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleQueries.DATE_IN_VEHICLE_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleQueries.LAST_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleQueries.SELECT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleQueries.SELECT_VEHICLE_SEARCH
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleQueries.VEHICLE_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleQueries.VEHICLE_PRESENCE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleQueries.VEHICLE_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleQueries.WITH_VEHICLE_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IVehicleEntityRepository: ReactiveCrudRepository<VehicleEntity, UUID> {
    @Query(
        """
        WITH $WITH_VEHICLE_LAST_MOVEMENT
        SELECT t.*, $SELECT_LAST_MOVEMENT, $SELECT_VEHICLE_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $VEHICLE_TABLE t $LAST_MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VEHICLE_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $VEHICLE_AVAILABILITY_CLAUSE AND $VEHICLE_PRESENCE_CLAUSE AND $DATE_IN_VEHICLE_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$VEHICLE_BRAND
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        projectId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<VehicleEntity>

    @Query(
        """
        WITH $WITH_VEHICLE_LAST_MOVEMENT
        SELECT COUNT(t.$ID)
        FROM $VEHICLE_TABLE t $LAST_MOVEMENT_JOIN
        WHERE $PROJECT_CLAUSE AND $VEHICLE_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $VEHICLE_AVAILABILITY_CLAUSE AND $VEHICLE_PRESENCE_CLAUSE AND $DATE_IN_VEHICLE_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        projectId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        WITH $WITH_VEHICLE_LAST_MOVEMENT
        SELECT t.*, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $VEHICLE_TABLE t $LAST_MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        ORDER BY t.$VEHICLE_BRAND
        """
    )
    fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<VehicleEntity>

    @Query(
        """
        WITH $WITH_VEHICLE_LAST_MOVEMENT
        SELECT t.*, $SELECT_LAST_MOVEMENT, $SELECT_VEHICLE_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $VEHICLE_TABLE t $LAST_MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VEHICLE_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $VEHICLE_AVAILABILITY_CLAUSE AND $VEHICLE_PRESENCE_CLAUSE AND $DATE_IN_VEHICLE_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$VEHICLE_BRAND
        LIMIT :limit
        """
    )
    fun findWithLimit(
        projectId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
    ): Flux<VehicleEntity>

    @Query(
        """
        WITH $WITH_VEHICLE_LAST_MOVEMENT
        SELECT t.*, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $VEHICLE_TABLE t $LAST_MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<VehicleEntity>

    @Query(
        """
        WITH $WITH_VEHICLE_LAST_MOVEMENT
        SELECT t.$ID FROM $VEHICLE_TABLE t $LAST_MOVEMENT_JOIN
        WHERE (last_movement.$VEHICLE_LAST_MOVEMENT_DATE_TIME IS NULL OR last_movement.$VEHICLE_LAST_MOVEMENT_DATE_TIME < :dateThreshold) AND t.$LAST_MODIFIER_DATE < :dateThreshold
        """
    )
    fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
}
