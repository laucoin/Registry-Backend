package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationFields.COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationFields.COMMUNICATION_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LAST_MODIFIER_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_LAST_COMMUNICATION_AT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.ACTIVITY_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.ACTIVITY_MOVEMENT_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.ACTIVITY_MOVEMENT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.CURRENT_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.DATE_IN_ACTIVITY_MOVEMENT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.GROUP_BY_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.LAST_PARTICIPANT_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.MOVEMENT_ACTIVITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.MOVEMENT_LINKED_TO_ACTIVITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.MOVEMENT_TYPE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.SELECT_ACTIVITY_MOVEMENT_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.SELECT_LINKED_ACTIVITY
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.WITH_CURRENT_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Repository
interface IMovementEntityRepository : ReactiveCrudRepository<MovementEntity, UUID> {
	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAll(
		projectId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
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
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
	)
	fun countAll(
		projectId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<Long>

	@Query(
		"""
        WITH $WITH_CURRENT_MOVEMENT
        SELECT DISTINCT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $CURRENT_MOVEMENT_JOIN $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
	)
	fun findCurrent(
		projectId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<MovementEntity>

	@Query(
		"""
        WITH $WITH_CURRENT_MOVEMENT
        SELECT COUNT(DISTINCT t.$ID)
        FROM $MOVEMENT_TABLE t $CURRENT_MOVEMENT_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
	)
	fun countCurrent(
		projectId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<Long>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_CONTENT_TABLE mct
        INNER JOIN $MOVEMENT_TABLE t ON mct.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND mct.$MOVEMENT_CONTENT_PARTICIPANT_ID = :participantId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAllByParticipantId(
		projectId: UUID,
		participantId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
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
        WHERE $PROJECT_CLAUSE AND mct.$MOVEMENT_CONTENT_PARTICIPANT_ID = :participantId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
	)
	fun countAllByParticipantId(
		projectId: UUID,
		participantId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<Long>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_CONTENT_TABLE mct
        INNER JOIN $MOVEMENT_TABLE t ON mct.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND mct.$MOVEMENT_CONTENT_VEHICLE_ID = :vehicleId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAllByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
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
        WHERE $PROJECT_CLAUSE AND mct.$MOVEMENT_CONTENT_VEHICLE_ID = :vehicleId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_ACTIVITY_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
	)
	fun countAllByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<Long>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$MOVEMENT_ACTIVITY_ID = :activityId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAllByActivityId(
		projectId: UUID,
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
        WHERE $PROJECT_CLAUSE AND t.$MOVEMENT_ACTIVITY_ID = :activityId AND $VISIBLE_CLAUSE AND $MOVEMENT_TYPE_CLAUSE AND $MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
        """
	)
	fun countAllByActivityId(
		projectId: UUID,
		activityId: UUID,
		visibilitySearched: Boolean?,
		typeSearched: List<MovementTypeEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<Long>

	@Query(
		"""
        SELECT t.*, $SELECT_ACTIVITY_MOVEMENT_SEARCH, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $LAST_PARTICIPANT_MOVEMENT_JOIN $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $ACTIVITY_MOVEMENT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $ACTIVITY_MOVEMENT_AVAILABILITY_CLAUSE AND $DATE_IN_ACTIVITY_MOVEMENT_DATES_RANGE_CLAUSE
        GROUP BY $GROUP_BY_MOVEMENT
        ORDER BY similarity_score DESC, $MOVEMENT_ACTIVITY_NAME
        LIMIT :limit
        """
	)
	fun findByActivityWithLimit(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<MovementEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
	)
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementEntity>

	/**
	 * Ongoing activity outings are the activity-linked exits among the CURRENT
	 * movements — the same `current_movement` the board and the paginated list read,
	 * so an outing appears, and stops appearing, on exactly the same rule as
	 * everything else. What this query alone adds is the max communication date_time,
	 * which lets the dashboard run its "since last communication" chronometer.
	 */
	@Query(
		"""
        WITH $WITH_CURRENT_MOVEMENT
        SELECT DISTINCT t.*,
            (
                SELECT MAX(c.$COMMUNICATION_DATE_TIME)
                FROM $COMMUNICATION_TABLE c
                WHERE c.$COMMUNICATION_MOVEMENT_ID = t.$ID AND c.$VISIBLE IS TRUE
            ) AS $MOVEMENT_LAST_COMMUNICATION_AT,
            $SELECT_LINKED_ACTIVITY, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t
        $CURRENT_MOVEMENT_JOIN
        $ACTIVITY_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $MOVEMENT_LINKED_TO_ACTIVITY_CLAUSE
            AND t.$MOVEMENT_TYPE = 'OUT'
        ORDER BY t.$MOVEMENT_DATE_TIME DESC
        LIMIT :limit
        """
	)
	fun findOngoingActivities(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<MovementEntity>

	@Query(
		"""
        SELECT t.$ID
        FROM $MOVEMENT_TABLE t
        LEFT JOIN (
            SELECT MAX(tc.$COMMUNICATION_DATE_TIME), tc.$COMMUNICATION_MOVEMENT_ID FROM $COMMUNICATION_TABLE tc
            WHERE tc.$COMMUNICATION_MOVEMENT_ID IS NOT NULL
            GROUP BY tc.$COMMUNICATION_MOVEMENT_ID
        ) lc ON lc.$COMMUNICATION_MOVEMENT_ID = t.$ID
        WHERE (lc.max IS NULL OR lc.max < :dateThreshold) AND t.$LAST_MODIFIER_DATE < :dateThreshold
        """
	)
	fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID>
}
