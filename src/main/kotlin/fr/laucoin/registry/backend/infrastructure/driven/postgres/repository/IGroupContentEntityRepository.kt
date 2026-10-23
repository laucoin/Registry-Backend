package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupQueries.CONTENT_TO_CONTENT_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupQueries.SELECT_CONTENT_TO_CONTENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_EFFECTIVE_DEPARTURE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupOnSiteCountsEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupQueries.SELECT_ON_SITE_MEMBERS_COUNTS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_EFFECTIVE_ARRIVAL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.GROUPS_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.LAST_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.PROJECT_CLAUSE
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IGroupContentEntityRepository: ReactiveCrudRepository<GroupContentEntity, UUID> {
	@Query(
		"""
        SELECT t.*, $SELECT_CONTENT_TO_CONTENT
        FROM $GROUP_CONTENT_TABLE t
        INNER JOIN $GROUP_TABLE gt ON t.$GROUP_CONTENT_GROUP_ID = gt.$ID $CONTENT_TO_CONTENT_JOIN
        WHERE gt.$LINKED_PROJECT_ID = :projectId AND t.$GROUP_CONTENT_GROUP_ID IN (:groupIds) AND (:isVisible IS NULL OR $PARTICIPANT_TABLE.$VISIBLE = :isVisible) AND (
            :isAvailable IS NULL OR :isAvailable = (
                (
                    COALESCE($PARTICIPANT_TABLE.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) < CAST(:#{now} AS DATE)
                    OR (COALESCE($PARTICIPANT_TABLE.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:#{now} AS DATE) AND COALESCE($PARTICIPANT_TABLE.$PARTICIPANT_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CAST(:#{now} AS TIMETZ))
                ) AND
                (
                    COALESCE($PARTICIPANT_TABLE.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) > CAST(:#{now} AS DATE)
                    OR (COALESCE($PARTICIPANT_TABLE.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:#{now} AS DATE) AND COALESCE($PARTICIPANT_TABLE.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CAST(:#{now} AS TIMETZ))
                ) AND
                $PARTICIPANT_TABLE.$PARTICIPANT_EFFECTIVE_DEPARTURE IS NULL
            )
        )
    """
	)
	fun findAllByGroupIds(
		projectId: UUID,
		groupIds: List<UUID>,
		isVisible: Boolean?,
		isAvailable: Boolean?
	): Flux<GroupContentEntity>

	@Query(
		"""
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT gc.$GROUP_CONTENT_GROUP_ID, $SELECT_ON_SITE_MEMBERS_COUNTS
        FROM $PARTICIPANT_TABLE t
        INNER JOIN $GROUP_CONTENT_TABLE gc ON gc.$GROUP_CONTENT_PARTICIPANT_ID = t.$ID
        $LAST_MOVEMENT_JOIN $GROUPS_JOIN
        WHERE $PROJECT_CLAUSE AND t.$VISIBLE IS TRUE AND gc.$GROUP_CONTENT_GROUP_ID IN (:groupIds)
            AND t.$PARTICIPANT_EFFECTIVE_ARRIVAL IS NOT NULL AND t.$PARTICIPANT_EFFECTIVE_DEPARTURE IS NULL
        GROUP BY gc.$GROUP_CONTENT_GROUP_ID
    """
	)
	fun findOnSiteCounts(projectId: UUID, groupIds: List<UUID>, at: ZonedDateTime?): Flux<GroupOnSiteCountsEntity>

	@Query("DELETE FROM $GROUP_CONTENT_TABLE WHERE $GROUP_CONTENT_GROUP_ID = :groupId AND $GROUP_CONTENT_PARTICIPANT_ID IN (:participantIds)")
	fun deleteAllByGroupIdAndParticipantIds(groupId: UUID, participantIds: List<UUID>): Mono<Unit>

	@Query("DELETE FROM $GROUP_CONTENT_TABLE WHERE $GROUP_CONTENT_GROUP_ID = :groupId AND $GROUP_CONTENT_PARTICIPANT_ID IN (:participantIds)")
	fun deleteAllByParticipantIdAndGroupIds(participantId: UUID, groupIds: List<UUID>): Mono<Unit>
}
