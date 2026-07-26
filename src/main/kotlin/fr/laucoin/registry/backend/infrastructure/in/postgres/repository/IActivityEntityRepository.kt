package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityQueries.ACTIVITY_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityQueries.ACTIVITY_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityQueries.DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityQueries.SELECT_ACTIVITY_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LAST_MODIFIER_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
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
interface IActivityEntityRepository : ReactiveCrudRepository<ActivityEntity, UUID> {
	@Query(
		"""
        SELECT t.*, $SELECT_ACTIVITY_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $ACTIVITY_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $ACTIVITY_AVAILABILITY_CLAUSE AND $DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$ACTIVITY_NAME
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<ActivityEntity>

	@Query(
		"""
        SELECT COUNT(t.$ID)
        FROM $ACTIVITY_TABLE t
        WHERE $PROJECT_CLAUSE AND $ACTIVITY_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $ACTIVITY_AVAILABILITY_CLAUSE AND $DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
        """
	)
	fun countAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<Long>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
	)
	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_ACTIVITY_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $ACTIVITY_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $ACTIVITY_AVAILABILITY_CLAUSE AND $DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$ACTIVITY_NAME
        LIMIT :limit
        """
	)
	fun findWithLimit(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<ActivityEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
	)
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityEntity>

	@Query(
		"""
        SELECT t.$ID
        FROM $ACTIVITY_TABLE t
        LEFT JOIN (
            SELECT MAX(tm.$MOVEMENT_DATE_TIME), tm.$MOVEMENT_ACTIVITY_ID FROM $MOVEMENT_TABLE tm
            WHERE tm.$MOVEMENT_ACTIVITY_ID IS NOT NULL
            GROUP BY tm.$MOVEMENT_ACTIVITY_ID
        ) lu ON lu.$MOVEMENT_ACTIVITY_ID = t.$ID
        WHERE (lu.max IS NULL OR lu.max < :dateThreshold) AND t.$LAST_MODIFIER_DATE < :dateThreshold
        """
	)
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
}
