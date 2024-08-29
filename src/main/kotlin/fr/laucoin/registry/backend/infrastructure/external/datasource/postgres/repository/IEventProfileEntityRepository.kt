package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileQueries.IN_DATE_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileQueries.JOIN_USER
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileQueries.STATUS_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileQueries.USABLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ENTITY_ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.EVENT_ROLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_LEVEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.EVENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LINKED_EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.ONLY_VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LINKED_EVENT
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
        WHERE $ONLY_VISIBLE_CLAUSE AND $STATUS_CLAUSE AND $USABLE_CLAUSE AND $IN_DATE_RANGE_CLAUSE AND t.$EVENT_PROFILE_USER_ID = :userId
        """
    )
    fun findByUserId(
        userId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?,
    ): Flux<EventProfileEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $EVENT_TABLE $LINKED_EVENT_TABLE ON t.$LINKED_EVENT_ID = $LINKED_EVENT_TABLE.$ID
        WHERE $STATUS_CLAUSE AND $USABLE_CLAUSE AND t.$EVENT_PROFILE_USER_ID = :userId
        """
    )
    fun findAllByUserId(
        userId: UUID,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
    ): Flux<EventProfileEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER
        INNER JOIN $EVENT_TABLE $LINKED_EVENT_TABLE ON t.$LINKED_EVENT_ID = $LINKED_EVENT_TABLE.$ID
        $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND $STATUS_CLAUSE AND $USABLE_CLAUSE AND t.$EVENT_PROFILE_USER_ID = :userId AND $LINKED_EVENT_ID = :eventId
        """
    )
    fun findUsableProfileByEventAndUserId(
        userId: UUID,
        eventId: UUID,
        onlyVisible: Boolean = true,
        status: ProfileStatusEnum = ProfileStatusEnum.ACCEPTED,
        onlyUsable: Boolean = true,
    ): Mono<EventProfileEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND $STATUS_CLAUSE AND $USABLE_CLAUSE AND $IN_DATE_RANGE_CLAUSE AND t.$LINKED_EVENT_ID = :eventId
        """
    )
    fun findByEventId(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        startAccess: ZonedDateTime?,
        endAccess: ZonedDateTime?,
    ): Flux<EventProfileEntity>

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
        WHERE $ONLY_VISIBLE_CLAUSE
        AND t.$EVENT_PROFILE_STATUS = 'ACCEPTED'
        AND (t.$EVENT_PROFILE_START_ACCESS IS NULL OR t.$EVENT_PROFILE_START_ACCESS <= CURRENT_TIMESTAMP)
        AND (t.$EVENT_PROFILE_END_ACCESS IS NULL OR t.$EVENT_PROFILE_END_ACCESS >= CURRENT_TIMESTAMP)
        AND $EVENT_ROLE_TABLE.$ROLE_LEVEL = 0
        GROUP BY t.$LINKED_EVENT_ID, $LINKED_EVENT_NAME
        """
    )
    fun findLevel0EventProfileRoleByUserId(userId: UUID, onlyVisible: Boolean): Flux<EventProfileRoleCountEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $EVENT_ROLE_TABLE ON t.$EVENT_PROFILE_ROLE = $EVENT_ROLE_TABLE.$ENTITY_ROLE_NAME
        WHERE $ONLY_VISIBLE_CLAUSE
        AND $EVENT_ROLE_TABLE.$ROLE_LEVEL = 0
        AND t.$LINKED_EVENT_ID = :eventId
        """
    )
    fun findLevel0EventProfileRoleByEventId(eventId: UUID, onlyVisible: Boolean): Flux<EventProfileEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE
        AND $STATUS_CLAUSE
        AND $USABLE_CLAUSE
        AND t.$LINKED_EVENT_ID = :eventId
        AND t.$EVENT_PROFILE_USER_ID = :userId
        LIMIT 1
        """
    )
    fun findEventProfileByEventAndUserId(
        eventId: UUID,
        userId: UUID,
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
    ): Mono<EventProfileEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND t.$EVENT_PROFILE_USER_ID = :userId AND t.$ID = :id
        """
    )
    fun findByIdAndUserId(
        userId: UUID,
        id: UUID,
        onlyVisible: Boolean,
    ): Mono<EventProfileEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $EVENT_PROFILE_TABLE t $JOIN_USER $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND t.$LINKED_EVENT_ID = :eventId AND t.$ID = :id
        """
    )
    fun findByIdAndEventId(
        eventId: UUID,
        id: UUID,
        onlyVisible: Boolean,
    ): Mono<EventProfileEntity>
}
