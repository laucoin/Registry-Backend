package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.DATES_IN_EVENT_PROFILE_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.DATE_IN_EVENT_PROFILE_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.EVENT_PROFILE_STATUS_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.EVENT_PROFILE_TEXT_EVENT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.EVENT_PROFILE_TEXT_USER_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.EVENT_PROFILE_USABLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.JOIN_USER
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.LINKED_USER_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.SELECT_EVENT_PROFILE_USER_SEARCH
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileRoleEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ENTITY_ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.EVENT_ROLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_LEVEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.EVENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LINKED_EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LINKED_EVENT
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
interface IEventProfileEntityRepository: ReactiveCrudRepository<EventProfileEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$EVENT_PROFILE_USER_ID = :userId AND $EVENT_PROFILE_TEXT_EVENT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $EVENT_PROFILE_USABLE_CLAUSE AND $EVENT_PROFILE_STATUS_CLAUSE AND $DATE_IN_EVENT_PROFILE_DATES_RANGE_CLAUSE
        ORDER BY $LINKED_EVENT_TABLE.$EVENT_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findByUserId(
        userId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        statusSearched: List<ProfileStatusEnum>,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<EventProfileEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $EVENT_PROFILE_TABLE t $EVENT_JOIN
        WHERE t.$EVENT_PROFILE_USER_ID = :userId AND $EVENT_PROFILE_TEXT_EVENT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $EVENT_PROFILE_USABLE_CLAUSE AND $EVENT_PROFILE_STATUS_CLAUSE AND $DATE_IN_EVENT_PROFILE_DATES_RANGE_CLAUSE
        """
    )
    fun countByUserId(
        userId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        statusSearched: List<ProfileStatusEnum>,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_EVENT_PROFILE_USER_SEARCH, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$LINKED_EVENT_ID = :eventId AND $EVENT_PROFILE_TEXT_USER_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $EVENT_PROFILE_USABLE_CLAUSE AND $EVENT_PROFILE_STATUS_CLAUSE AND $DATE_IN_EVENT_PROFILE_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, $LINKED_USER_TABLE.$USER_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findByEventId(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        statusSearched: List<ProfileStatusEnum>,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<EventProfileEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN
        WHERE t.$LINKED_EVENT_ID = :eventId AND $EVENT_PROFILE_TEXT_USER_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $EVENT_PROFILE_USABLE_CLAUSE AND $EVENT_PROFILE_STATUS_CLAUSE AND $DATE_IN_EVENT_PROFILE_DATES_RANGE_CLAUSE
        """
    )
    fun countByEventId(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        statusSearched: List<ProfileStatusEnum>,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT DISTINCT t.$EVENT_PROFILE_USER_ID
        FROM $EVENT_PROFILE_TABLE t
        WHERE t.$LINKED_EVENT_ID = :eventId AND t.$EVENT_PROFILE_USER_ID IN (:userIds) AND (:profileIdToExclude IS NULL OR t.$ID != :profileIdToExclude) AND $EVENT_PROFILE_STATUS_CLAUSE AND $DATES_IN_EVENT_PROFILE_DATES_RANGE_CLAUSE
        """
    )
    fun findUserIdsWithEventProfileForEventWithProfileExclusion(
        eventId: UUID,
        userIds: List<UUID>,
        profileIdToExclude: UUID?,
        statusSearched: List<ProfileStatusEnum>,
        startDateTimeSearched: LocalDateTime?,
        endDateTimeSearched: LocalDateTime?,
    ): Flux<UUID>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_EVENT
        FROM $EVENT_PROFILE_TABLE t INNER JOIN $EVENT_TABLE $LINKED_EVENT_TABLE ON t.$LINKED_EVENT_ID = $LINKED_EVENT_TABLE.$ID
        WHERE t.$EVENT_PROFILE_USER_ID = :userId AND $VISIBLE_CLAUSE AND $EVENT_PROFILE_USABLE_CLAUSE AND $EVENT_PROFILE_STATUS_CLAUSE
        """
    )
    fun findAllRolesByUserId(
        userId: UUID,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        statusSearched: List<ProfileStatusEnum>,
    ): Flux<EventProfileRoleEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER
        INNER JOIN $EVENT_TABLE $LINKED_EVENT_TABLE ON t.$LINKED_EVENT_ID = $LINKED_EVENT_TABLE.$ID
        $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$EVENT_PROFILE_USER_ID = :userId AND $LINKED_EVENT_ID = :eventId AND $VISIBLE_CLAUSE AND $EVENT_PROFILE_USABLE_CLAUSE AND $EVENT_PROFILE_STATUS_CLAUSE
        """
    )
    fun findEventProfileByEventAndUserId(
        eventId: UUID,
        userId: UUID,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        statusSearched: List<ProfileStatusEnum>,
    ): Mono<EventProfileEntity>

    @Query(
        """
        SELECT t.$LINKED_EVENT_ID, $EVENT_TABLE.$EVENT_NAME AS $LINKED_EVENT_NAME, COUNT(t.$EVENT_PROFILE_ROLE)
        FROM $EVENT_PROFILE_TABLE t
        INNER JOIN $EVENT_TABLE ON t.$LINKED_EVENT_ID = $EVENT_TABLE.$ID AND $EVENT_TABLE.$VISIBLE IS TRUE
        INNER JOIN $EVENT_ROLE_TABLE ON t.$EVENT_PROFILE_ROLE = $EVENT_ROLE_TABLE.$ENTITY_ROLE_NAME
        INNER JOIN (
            SELECT ep.$LINKED_EVENT_ID AS user_event_id FROM $EVENT_PROFILE_TABLE ep
            WHERE ep.$VISIBLE IS TRUE AND ep.$EVENT_PROFILE_STATUS = 'ACCEPTED' AND ep.$EVENT_PROFILE_USER_ID = :userId
        ) AS euei ON t.$LINKED_EVENT_ID = euei.user_event_id
        WHERE $VISIBLE_CLAUSE
        AND t.$EVENT_PROFILE_STATUS = 'ACCEPTED'
        AND (COALESCE(t.$EVENT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) < CURRENT_DATE OR (COALESCE(t.$EVENT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$EVENT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME))
        AND (COALESCE(t.$EVENT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) > CURRENT_DATE OR (COALESCE(t.$EVENT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$EVENT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME))
        AND $EVENT_ROLE_TABLE.$ROLE_LEVEL = 0
        GROUP BY t.$LINKED_EVENT_ID, $EVENT_TABLE.$EVENT_NAME
        """
    )
    fun findLevel0EventProfileRoleByUserId(userId: UUID, visibilitySearched: Boolean?): Flux<EventProfileRoleCountEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $EVENT_ROLE_TABLE ON t.$EVENT_PROFILE_ROLE = $EVENT_ROLE_TABLE.$ENTITY_ROLE_NAME
        WHERE $VISIBLE_CLAUSE
        AND $EVENT_ROLE_TABLE.$ROLE_LEVEL = 0
        AND t.$LINKED_EVENT_ID = :eventId
        """
    )
    fun findLevel0EventProfileRoleByEventId(eventId: UUID, visibilitySearched: Boolean?): Flux<EventProfileEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$EVENT_PROFILE_USER_ID = :userId AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findByUserIdAndId(
        userId: UUID,
        id: UUID,
        visibilitySearched: Boolean?,
    ): Mono<EventProfileEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$LINKED_EVENT_ID = :eventId AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findByEventIdAndId(
        eventId: UUID,
        id: UUID,
        visibilitySearched: Boolean?,
    ): Mono<EventProfileEntity>
}
