package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventQueries.DATE_IN_EVENT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventQueries.EVENT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventRelationEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IEventEntityRepository: ReactiveCrudRepository<EventEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $EVENT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_EVENT_DATES_RANGE_CLAUSE
        ORDER BY t.$EVENT_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<EventEntity>

    @Query(
        """
        SELECT COUNT(t.$ID) FROM $EVENT_TABLE t
        WHERE $EVENT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_EVENT_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $EVENT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$ID IN (:eventIds) AND $EVENT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_EVENT_DATES_RANGE_CLAUSE
        ORDER BY t.$EVENT_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllInEventIds(
        eventIds: List<UUID>,
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<EventEntity>

    @Query(
        """
        SELECT COUNT(t.$ID) FROM $EVENT_TABLE t
        WHERE t.$ID IN (:eventIds) AND $EVENT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_EVENT_DATES_RANGE_CLAUSE
        """
    )
    fun countAllInEventIds(
        eventIds: List<UUID>,
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $EVENT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(id: UUID, visibilitySearched: Boolean?): Mono<EventEntity>

    @Query(
        """
        SELECT COUNT(t.$ID) FROM (
            SELECT t.$ID FROM $EVENT_PROFILE_TABLE t WHERE t.$LINKED_EVENT_ID = :id AND (
                COALESCE(t.$EVENT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$EVENT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$EVENT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$EVENT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$EVENT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$EVENT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )

            UNION

            SELECT t.$ID FROM $MOVEMENT_TABLE t WHERE t.$LINKED_EVENT_ID = :id AND (t.$MOVEMENT_DATE_TIME < :begin OR t.$MOVEMENT_DATE_TIME > :end)

            UNION

            SELECT t.$ID FROM $PARTICIPANT_TABLE t WHERE t.$LINKED_EVENT_ID = :id AND (
                COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )
        
            UNION
        
            SELECT t.$ID FROM $GROUP_TABLE t WHERE t.$LINKED_EVENT_ID = :id AND (
                COALESCE(t.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )
        
            UNION
        
            SELECT t.$ID FROM $VEHICLE_TABLE t WHERE t.$LINKED_EVENT_ID = :id AND (
                COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )
        
            UNION
        
            SELECT t.$ID FROM $ACTIVITY_TABLE t WHERE t.$LINKED_EVENT_ID = :id AND (
                COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )
        ) AS t
        """
    )
    fun validDateTime(id: UUID, begin: LocalDateTime?, end: LocalDateTime?): Mono<EventRelationEntity>
}
