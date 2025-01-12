package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_TABLE
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface IGroupContentEntityRepository: ReactiveCrudRepository<GroupContentEntity, UUID> {
    @Query("DELETE FROM $GROUP_CONTENT_TABLE WHERE $GROUP_CONTENT_GROUP_ID = :groupId AND $GROUP_CONTENT_PARTICIPANT_ID IN (:participantIds)")
    fun deleteAllByGroupIdAndParticipantIds(groupId: UUID, participantIds: List<UUID>): Mono<Void>

    @Query("DELETE FROM $GROUP_CONTENT_TABLE WHERE $GROUP_CONTENT_GROUP_ID = :groupId AND $GROUP_CONTENT_PARTICIPANT_ID IN (:participantIds)")
    fun deleteAllByParticipantIdAndGroupIds(participantId: UUID, groupIds: List<UUID>): Mono<Void>
}
