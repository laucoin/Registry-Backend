package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.CONTENT_TO_CONTENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.SELECT_CONTENT_TO_CONTENT
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
        WHERE gt.$LINKED_EVENT_ID = :eventId AND t.$GROUP_CONTENT_GROUP_ID IN (:groupIds)
    """
    )
    fun findAllByGroupIds(eventId: UUID, groupIds: List<UUID>): Flux<GroupContentEntity>

    @Query("DELETE FROM $GROUP_CONTENT_TABLE WHERE $GROUP_CONTENT_GROUP_ID = :groupId AND $GROUP_CONTENT_PARTICIPANT_ID IN (:participantIds)")
    fun deleteAllByGroupIdAndParticipantIds(groupId: UUID, participantIds: List<UUID>): Mono<Void>

    @Query("DELETE FROM $GROUP_CONTENT_TABLE WHERE $GROUP_CONTENT_GROUP_ID = :groupId AND $GROUP_CONTENT_PARTICIPANT_ID IN (:participantIds)")
    fun deleteAllByParticipantIdAndGroupIds(participantId: UUID, groupIds: List<UUID>): Mono<Void>
}
