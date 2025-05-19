package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.alert.AlertFields.ALERT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.alert.AlertFields.ALERT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.communication.CommunicationFields.COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.communication.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
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
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectQueries.DATE_IN_PROJECT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectQueries.PROJECT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectRelationEntity
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
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IProjectEntityRepository: ReactiveCrudRepository<ProjectEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $PROJECT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_PROJECT_DATES_RANGE_CLAUSE
        ORDER BY t.$PROJECT_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<ProjectEntity>

    @Query(
        """
        SELECT COUNT(t.$ID) FROM $PROJECT_TABLE t
        WHERE $PROJECT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_PROJECT_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $PROJECT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$ID IN (:projectIds) AND $PROJECT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_PROJECT_DATES_RANGE_CLAUSE
        ORDER BY t.$PROJECT_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllInProjectIds(
        projectIds: List<UUID>,
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<ProjectEntity>

    @Query(
        """
        SELECT COUNT(t.$ID) FROM $PROJECT_TABLE t
        WHERE t.$ID IN (:projectIds) AND $PROJECT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $DATE_IN_PROJECT_DATES_RANGE_CLAUSE
        """
    )
    fun countAllInProjectIds(
        projectIds: List<UUID>,
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $PROJECT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(id: UUID, visibilitySearched: Boolean?): Mono<ProjectEntity>

    @Query(
        """
        SELECT COUNT(t.$ID) FROM (
            SELECT t.$ID FROM $PROJECT_PROFILE_TABLE t WHERE t.$LINKED_PROJECT_ID = :id AND (
                COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )

            UNION

            SELECT t.$ID FROM $MOVEMENT_TABLE t WHERE t.$LINKED_PROJECT_ID = :id AND (t.$MOVEMENT_DATE_TIME < :begin OR t.$MOVEMENT_DATE_TIME > :end)

            UNION

            SELECT t.$ID FROM $PARTICIPANT_TABLE t WHERE t.$LINKED_PROJECT_ID = :id AND (
                COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )

            UNION

            SELECT t.$ID FROM $GROUP_TABLE t WHERE t.$LINKED_PROJECT_ID = :id AND (
                COALESCE(t.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )

            UNION

            SELECT t.$ID FROM $VEHICLE_TABLE t WHERE t.$LINKED_PROJECT_ID = :id AND (
                COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )

            UNION

            SELECT t.$ID FROM $ACTIVITY_TABLE t WHERE t.$LINKED_PROJECT_ID = :id AND (
                COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) > CAST(:end AS DATE) OR (COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) > CAST(:end AS TIME))
                OR COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) < CAST(:begin AS DATE) OR (COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:end AS DATE) AND COALESCE(t.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) < CAST(:end AS TIME))
            )

            UNION

            SELECT t.$ID FROM $COMMUNICATION_TABLE t WHERE t.$LINKED_PROJECT_ID = :id AND (t.$COMMUNICATION_DATE_TIME < :begin OR t.$COMMUNICATION_DATE_TIME > :end)

            UNION

            SELECT t.$ID FROM $ALERT_TABLE t WHERE t.$LINKED_PROJECT_ID = :id AND (t.$ALERT_DATE_TIME < :begin OR t.$ALERT_DATE_TIME > :end)
        ) AS t
        """
    )
    fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<ProjectRelationEntity>
}
