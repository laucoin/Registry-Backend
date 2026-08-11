package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.DATE_IN_GROUP_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_ARRIVING_TODAY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_DEPARTING_TODAY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_INSIDE_MEMBERS_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_MEMBERS_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_PRESENCE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.GROUP_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.SELECT_MEMBERS_COUNTS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.WITH_GROUP_INSIDE_MEMBERS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.WITH_GROUP_MEMBERS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupQueries.WITH_PARTICIPANT_GROUPS
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
import java.time.ZonedDateTime
import java.util.UUID

@Repository
interface IGroupEntityRepository : ReactiveCrudRepository<GroupEntity, UUID> {
	@Query(
		"""
        WITH $WITH_PARTICIPANT_GROUPS, $WITH_GROUP_INSIDE_MEMBERS, $WITH_GROUP_MEMBERS
        SELECT t.*, $SELECT_MEMBERS_COUNTS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $GROUP_INSIDE_MEMBERS_JOIN $GROUP_MEMBERS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $GROUP_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_PRESENCE_CLAUSE AND $DATE_IN_GROUP_DATES_RANGE_CLAUSE
        ORDER BY t.$GROUP_NAME
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<GroupEntity>

	@Query(
		"""
        SELECT COUNT(t.$ID)
        FROM $GROUP_TABLE t
        WHERE $PROJECT_CLAUSE AND $GROUP_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_PRESENCE_CLAUSE AND $DATE_IN_GROUP_DATES_RANGE_CLAUSE
        """
	)
	fun countAll(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<Long>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_GROUPS, $WITH_GROUP_INSIDE_MEMBERS, $WITH_GROUP_MEMBERS
        SELECT t.*, $SELECT_MEMBERS_COUNTS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $GROUP_INSIDE_MEMBERS_JOIN $GROUP_MEMBERS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
	)
	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<GroupEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_GROUPS, $WITH_GROUP_INSIDE_MEMBERS, $WITH_GROUP_MEMBERS
        SELECT t.*, $SELECT_MEMBERS_COUNTS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $GROUP_INSIDE_MEMBERS_JOIN $GROUP_MEMBERS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $GROUP_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_PRESENCE_CLAUSE AND $DATE_IN_GROUP_DATES_RANGE_CLAUSE
        ORDER BY t.$GROUP_NAME
        LIMIT :limit
        """
	)
	fun findWithLimit(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
	): Flux<GroupEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_GROUPS, $WITH_GROUP_INSIDE_MEMBERS, $WITH_GROUP_MEMBERS
        SELECT t.*, $SELECT_MEMBERS_COUNTS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $GROUP_INSIDE_MEMBERS_JOIN $GROUP_MEMBERS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
	)
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<GroupEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_GROUPS, $WITH_GROUP_INSIDE_MEMBERS, $WITH_GROUP_MEMBERS
        SELECT t.*, $SELECT_MEMBERS_COUNTS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $GROUP_INSIDE_MEMBERS_JOIN $GROUP_MEMBERS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_ARRIVING_TODAY_CLAUSE
        ORDER BY t.$GROUP_NAME
        LIMIT :limit
        """
	)
	fun findArrivingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<GroupEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_GROUPS, $WITH_GROUP_INSIDE_MEMBERS, $WITH_GROUP_MEMBERS
        SELECT t.*, $SELECT_MEMBERS_COUNTS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $GROUP_INSIDE_MEMBERS_JOIN $GROUP_MEMBERS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_DEPARTING_TODAY_CLAUSE
        ORDER BY t.$GROUP_NAME
        LIMIT :limit
        """
	)
	fun findDepartingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<GroupEntity>

	@Query(
		"""
        SELECT t.$ID FROM $GROUP_TABLE t
        LEFT JOIN (
            SELECT t.$GROUP_CONTENT_GROUP_ID, COUNT(t.$ID) FROM $GROUP_CONTENT_TABLE t
            WHERE t.$GROUP_CONTENT_PARTICIPANT_ID NOT IN (:participantToExclude)
            GROUP BY t.$GROUP_CONTENT_GROUP_ID
        ) gc ON t.$ID = gc.$GROUP_CONTENT_GROUP_ID
        WHERE gc.count IS NULL OR gc.count = 0
        """
	)
	fun findEmpty(participantToExclude: List<UUID>): Flux<UUID>
}
