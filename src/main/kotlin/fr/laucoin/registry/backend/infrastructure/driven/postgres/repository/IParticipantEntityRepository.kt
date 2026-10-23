package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.CREATED_AT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.FULL_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.GROUP_DRIVES_PARTICIPANT_PLANNED_DATE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.GROUP_PLANNED_DATE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_OWN_PLANNED_DATE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_PLANNED_ELIGIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_WITHOUT_OWN_PLANNED_DATE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.GROUPS_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.LAST_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_BIRTHDAY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_MAJOR_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_PRESENCE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_ON_SITE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_SORT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_STATUS_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TYPE_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_USER_LINK_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.SELECT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.UPDATE_PARTICIPANT_EFFECTIVE_DATES
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_GROUPS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.SELECT_PARTICIPANT_SEARCH
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.USER_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_LAST_MOVEMENT_ALL_PROJECTS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Reactive entity repository of the participant table.
 * Scope: Holds the parameterized SQL queries (filters, sort, pagination, locks) on participant.
 * Limits: Used by the Postgres adapter only; returns entities, never domain models.
 */
@Repository
interface IParticipantEntityRepository : ReactiveCrudRepository<ParticipantEntity, UUID> {
	companion object {
		private const val MAX_UNPAGINATED_RESULT = 1000
	}

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR, COUNT(*) OVER() AS $FULL_COUNT
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE AND $PARTICIPANT_USER_LINK_CLAUSE AND $PARTICIPANT_ON_SITE_CLAUSE AND $PARTICIPANT_BIRTHDAY_CLAUSE
        ORDER BY $PARTICIPANT_SORT similarity_score DESC, t.$PARTICIPANT_LAST_NAME, t.$ID
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAll(
		projectId: UUID,
		query: String?,
		isMajor: Boolean?,
		type: ParticipantTypeEnum?,
		isVisible: Boolean?,
		isAvailable: Boolean?,
		status: PresenceStatusEnum?,
		presence: PresenceStatusEnum?,
		at: ZonedDateTime?,
		hasUser: Boolean?,
		isOnSite: Boolean?,
		birthdayFrom: LocalDate?,
		birthdayTo: LocalDate?,
		sortField: String?,
		isDescending: Boolean?,
		limit: Int,
		offset: Int,
	): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT COUNT(t.$ID)
        FROM $PARTICIPANT_TABLE t $LAST_MOVEMENT_JOIN $GROUPS_JOIN
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE AND $PARTICIPANT_USER_LINK_CLAUSE AND $PARTICIPANT_ON_SITE_CLAUSE AND $PARTICIPANT_BIRTHDAY_CLAUSE
        """
	)
	fun countAll(
		projectId: UUID,
		query: String?,
		isMajor: Boolean?,
		type: ParticipantTypeEnum?,
		isVisible: Boolean?,
		isAvailable: Boolean?,
		status: PresenceStatusEnum?,
		presence: PresenceStatusEnum?,
		at: ZonedDateTime?,
		hasUser: Boolean?,
		isOnSite: Boolean?,
		birthdayFrom: LocalDate?,
		birthdayTo: LocalDate?,
	): Mono<Long>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR, COUNT(*) OVER() AS $FULL_COUNT
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $GROUP_CONTENT_TABLE ON t.id = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID AND $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID = :groupId
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE AND $PARTICIPANT_USER_LINK_CLAUSE AND $PARTICIPANT_ON_SITE_CLAUSE AND $PARTICIPANT_BIRTHDAY_CLAUSE
        ORDER BY $PARTICIPANT_SORT similarity_score DESC, t.$PARTICIPANT_LAST_NAME, t.$ID
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAllByGroupId(
		projectId: UUID,
		groupId: UUID,
		query: String?,
		isMajor: Boolean?,
		type: ParticipantTypeEnum?,
		isVisible: Boolean?,
		isAvailable: Boolean?,
		status: PresenceStatusEnum?,
		presence: PresenceStatusEnum?,
		at: ZonedDateTime?,
		hasUser: Boolean?,
		isOnSite: Boolean?,
		birthdayFrom: LocalDate?,
		birthdayTo: LocalDate?,
		sortField: String?,
		isDescending: Boolean?,
		limit: Int,
		offset: Int,
	): Flux<ParticipantEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE
            AND EXTRACT(MONTH FROM t.$PARTICIPANT_BIRTHDAY) = EXTRACT(MONTH FROM CAST(:#{now} AS DATE))
            AND EXTRACT(DAY FROM t.$PARTICIPANT_BIRTHDAY) = EXTRACT(DAY FROM CAST(:#{now} AS DATE))
            AND $VISIBLE_CLAUSE
        LIMIT $MAX_UNPAGINATED_RESULT
        """
	)
	fun findAllWithBirthday(projectId: UUID, isVisible: Boolean?): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
	)
	fun findAllByIds(
		projectId: UUID,
		ids: List<UUID>,
		isVisible: Boolean?,
		at: ZonedDateTime?
	): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$PARTICIPANT_USER_ID = :userId
        LIMIT $MAX_UNPAGINATED_RESULT
        """
	)
	fun findByUserId(projectId: UUID, userId: UUID, at: ZonedDateTime?): Flux<ParticipantEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$PARTICIPANT_USER_ID = :userId
        LIMIT $MAX_UNPAGINATED_RESULT
        """
	)
	fun findAllByUserId(userId: UUID): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$PARTICIPANT_LAST_NAME
        LIMIT :limit
        """
	)
	fun findWithLimit(
		projectId: UUID,
		query: String?,
		isMajor: Boolean?,
		type: ParticipantTypeEnum?,
		isVisible: Boolean?,
		isAvailable: Boolean?,
		status: PresenceStatusEnum?,
		presence: PresenceStatusEnum?,
		at: ZonedDateTime?,
		limit: Int,
	): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
	)
	fun findById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?,
		at: ZonedDateTime?
	): Mono<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT
        SELECT t.$ID FROM $PARTICIPANT_TABLE t $LAST_MOVEMENT_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_PLANNED_ELIGIBLE_CLAUSE AND $PARTICIPANT_OWN_PLANNED_DATE_CLAUSE
        ORDER BY t.$PARTICIPANT_LAST_NAME, t.$ID
        LIMIT :limit
        """
	)
	fun findIdsPlannedOnOwnDate(
		projectId: UUID,
		isVisible: Boolean?,
		isArrival: Boolean,
		day: LocalDate,
		limit: Int,
	): Flux<UUID>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT
        SELECT DISTINCT ON (t.$ID) gc.$ID, gc.$GROUP_CONTENT_GROUP_ID, gc.$GROUP_CONTENT_PARTICIPANT_ID
        FROM $GROUP_CONTENT_TABLE gc
        INNER JOIN $GROUP_TABLE g ON g.$ID = gc.$GROUP_CONTENT_GROUP_ID AND g.$VISIBLE IS TRUE
        INNER JOIN $PARTICIPANT_TABLE t ON t.$ID = gc.$GROUP_CONTENT_PARTICIPANT_ID $LAST_MOVEMENT_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_PLANNED_ELIGIBLE_CLAUSE AND $PARTICIPANT_WITHOUT_OWN_PLANNED_DATE_CLAUSE
            AND $GROUP_PLANNED_DATE_CLAUSE AND $GROUP_DRIVES_PARTICIPANT_PLANNED_DATE_CLAUSE
        ORDER BY t.$ID, g.$GROUP_NAME
        LIMIT :limit
        """
	)
	fun findGroupContentsPlannedOnGroupDate(
		projectId: UUID,
		isVisible: Boolean?,
		isArrival: Boolean,
		day: LocalDate,
		limit: Int,
	): Flux<GroupContentEntity>

	@Query("SELECT t.$ID FROM $PARTICIPANT_TABLE t WHERE t.$ID IN (:ids) ORDER BY t.$ID FOR UPDATE OF t")
	fun lockByIds(ids: List<UUID>): Flux<UUID>

	@Modifying
	@Query(UPDATE_PARTICIPANT_EFFECTIVE_DATES)
	fun refreshEffectiveDates(ids: List<UUID>): Mono<Long>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT_ALL_PROJECTS
        SELECT t.$ID FROM $PARTICIPANT_TABLE t $LAST_MOVEMENT_JOIN
        WHERE (last_movement.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME IS NULL OR last_movement.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME < :dateThreshold) AND t.$CREATED_AT < :dateThreshold
        """
	)
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
}
