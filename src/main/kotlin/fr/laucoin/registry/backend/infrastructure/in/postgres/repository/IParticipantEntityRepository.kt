package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LAST_MODIFIER_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_DEPARTED_AT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.ARRIVING_TODAY_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.ARRIVING_TODAY_NOT_PRESENT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.DEPARTING_TODAY_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.DEPARTING_TODAY_PRESENT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.GROUPS_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.LAST_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_BIRTHDAY_TODAY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_DEPARTED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_GROUPED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_MAJOR_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_STATUS_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TYPE_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.PARTICIPANT_WARNED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.SELECT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_GROUPS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.SELECT_PARTICIPANT_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.USER_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.WITH_ARRIVING_TODAY
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.WITH_DEPARTING_TODAY
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_LAST_MOVEMENT
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
interface IParticipantEntityRepository : ReactiveCrudRepository<ParticipantEntity, UUID> {
	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_DEPARTED_CLAUSE AND $PARTICIPANT_WARNED_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE AND $PARTICIPANT_GROUPED_CLAUSE
        ORDER BY similarity_score DESC, t.$PARTICIPANT_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAll(
		projectId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: String?,
		departedSearched: Boolean?,
		warnedSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		groupedSearched: Boolean?,
		limit: Int,
		offset: Int,
	): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT COUNT(t.$ID)
        FROM $PARTICIPANT_TABLE t $LAST_MOVEMENT_JOIN $GROUPS_JOIN
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_DEPARTED_CLAUSE AND $PARTICIPANT_WARNED_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE AND $PARTICIPANT_GROUPED_CLAUSE
        """
	)
	fun countAll(
		projectId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: String?,
		departedSearched: Boolean?,
		warnedSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		groupedSearched: Boolean?,
	): Mono<Long>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $GROUP_CONTENT_TABLE ON t.id = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID AND $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID = :groupId
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_DEPARTED_CLAUSE AND $PARTICIPANT_WARNED_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$PARTICIPANT_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAllByGroupId(
		projectId: UUID,
		groupId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: String?,
		departedSearched: Boolean?,
		warnedSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT COUNT(t.$ID)
        FROM $PARTICIPANT_TABLE t $LAST_MOVEMENT_JOIN $GROUPS_JOIN
        INNER JOIN $GROUP_CONTENT_TABLE ON t.id = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID AND $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID = :groupId
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_DEPARTED_CLAUSE AND $PARTICIPANT_WARNED_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        """
	)
	fun countAllByGroupId(
		projectId: UUID,
		groupId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: String?,
		departedSearched: Boolean?,
		warnedSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<Long>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_BIRTHDAY_TODAY_CLAUSE AND $VISIBLE_CLAUSE
        LIMIT :limit
        """
	)
	fun findAllWithBirthday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_ARRIVING_TODAY
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN $ARRIVING_TODAY_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $ARRIVING_TODAY_NOT_PRESENT_CLAUSE
        ORDER BY t.$PARTICIPANT_LAST_NAME
        LIMIT :limit
        """
	)
	fun findArrivingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_DEPARTING_TODAY
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN $DEPARTING_TODAY_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $DEPARTING_TODAY_PRESENT_CLAUSE
        ORDER BY t.$PARTICIPANT_LAST_NAME
        LIMIT :limit
        """
	)
	fun findDepartingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
	)
	fun findAllByIds(
		projectId: UUID,
		ids: List<UUID>,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?
	): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$PARTICIPANT_USER_ID = :userId
        """
	)
	fun findByUserId(projectId: UUID, userId: UUID, dateTimeSearched: ZonedDateTime?): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_MAJOR_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_STATUS_CLAUSE AND $PARTICIPANT_DEPARTED_CLAUSE AND $PARTICIPANT_WARNED_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$PARTICIPANT_LAST_NAME
        LIMIT :limit
        """
	)
	fun findWithLimit(
		projectId: UUID,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: String?,
		departedSearched: Boolean?,
		warnedSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
	)
	fun findById(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?
	): Mono<ParticipantEntity>

	/**
	 * A terminal exit stamps `departed_at` and nothing else. It used to overwrite
	 * `end_availability_*` instead, which destroyed any date the staff had entered
	 * ahead of time and left "gone for good" indistinguishable from "the stay is
	 * over" — the two the register must keep apart.
	 */
	@Query(
		"""
        UPDATE $PARTICIPANT_TABLE SET $PARTICIPANT_DEPARTED_AT = :departedAt
        WHERE $ID IN (:ids)
        RETURNING *
        """
	)
	fun markAllAsDeparted(ids: List<UUID>, departedAt: ZonedDateTime): Flux<ParticipantEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT
        SELECT t.$ID FROM $PARTICIPANT_TABLE t $LAST_MOVEMENT_JOIN
        WHERE (last_movement.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME IS NULL OR last_movement.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME < :dateThreshold) AND t.$LAST_MODIFIER_DATE < :dateThreshold
        """
	)
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
}
