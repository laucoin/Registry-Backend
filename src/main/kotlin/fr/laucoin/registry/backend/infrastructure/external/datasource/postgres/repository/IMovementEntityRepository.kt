package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.ACTIVITY_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.MOVEMENT_TYPE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.SELECT_LINKED_ACTIVITY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.EVENT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.EVENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LINKED_EVENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IMovementEntityRepository: ReactiveCrudRepository<MovementEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $ACTIVITY_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        eventId: UUID,
        visibilitySearched: Boolean?,
        typeSearched: List<MovementTypeEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<MovementEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $MOVEMENT_TABLE t
        WHERE $EVENT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        eventId: UUID,
        visibilitySearched: Boolean?,
        typeSearched: List<MovementTypeEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_CONTENT_TABLE mct
        INNER JOIN $MOVEMENT_TABLE t ON mct.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID $ACTIVITY_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND mct.$MOVEMENT_CONTENT_PARTICIPANT_ID = :participantId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllByParticipantId(
        eventId: UUID,
        participantId: UUID,
        visibilitySearched: Boolean?,
        typeSearched: List<MovementTypeEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<MovementEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $MOVEMENT_CONTENT_TABLE mct
        INNER JOIN $MOVEMENT_TABLE t ON mct.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
        WHERE $EVENT_CLAUSE AND mct.$MOVEMENT_CONTENT_PARTICIPANT_ID = :participantId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
    )
    fun countAllByParticipantId(
        eventId: UUID,
        participantId: UUID,
        visibilitySearched: Boolean?,
        typeSearched: List<MovementTypeEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_CONTENT_TABLE mct
        INNER JOIN $MOVEMENT_TABLE t ON mct.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID $ACTIVITY_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND mct.$MOVEMENT_CONTENT_VEHICLE_ID = :vehicleId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllByVehicleId(
        eventId: UUID,
        vehicleId: UUID,
        visibilitySearched: Boolean?,
        typeSearched: List<MovementTypeEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<MovementEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $MOVEMENT_CONTENT_TABLE mct
        INNER JOIN $MOVEMENT_TABLE t ON mct.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
        WHERE $EVENT_CLAUSE AND mct.$MOVEMENT_CONTENT_VEHICLE_ID = :vehicleId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
    )
    fun countAllByVehicleId(
        eventId: UUID,
        vehicleId: UUID,
        visibilitySearched: Boolean?,
        typeSearched: List<MovementTypeEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $ACTIVITY_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND t.$MOVEMENT_ACTIVITY_ID = :activityId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllByActivityId(
        eventId: UUID,
        activityId: UUID,
        visibilitySearched: Boolean?,
        typeSearched: List<MovementTypeEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<MovementEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $MOVEMENT_TABLE t
        WHERE $EVENT_CLAUSE AND t.$MOVEMENT_ACTIVITY_ID = :activityId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
    )
    fun countAllByActivityId(
        eventId: UUID,
        activityId: UUID,
        visibilitySearched: Boolean?,
        typeSearched: List<MovementTypeEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $ACTIVITY_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementEntity>
}
