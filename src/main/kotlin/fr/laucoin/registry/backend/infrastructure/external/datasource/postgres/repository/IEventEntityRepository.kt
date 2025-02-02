package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventQueries.IN_DATE_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventRelationEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.ONLY_VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IEventEntityRepository: ReactiveCrudRepository<EventEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $EVENT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND $IN_DATE_RANGE_CLAUSE
        """
    )
    fun findAll(onlyVisible: Boolean, startDateTime: ZonedDateTime?, endDateTime: ZonedDateTime?): Flux<EventEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $EVENT_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND t.$ID = :id
        """
    )
    fun findById(id: UUID, onlyVisible: Boolean): Mono<EventEntity>

    @Query(
        """
        SELECT COUNT(t.$ID) FROM (
            SELECT tep.$ID FROM $EVENT_PROFILE_TABLE tep
            INNER JOIN $EVENT_TABLE te ON te.$ID = tep.$LINKED_EVENT_ID
            WHERE te.$ID = :id AND (tep.$EVENT_PROFILE_START_ACCESS IS NOT NULL OR tep.$EVENT_PROFILE_START_ACCESS >= :begin)
                AND (tep.$EVENT_PROFILE_END_ACCESS IS NOT NULL OR tep.$EVENT_PROFILE_END_ACCESS >= :end)
        
            UNION
            SELECT tm.$ID FROM $MOVEMENT_TABLE tm
            INNER JOIN $EVENT_TABLE te ON te.$ID = tm.$LINKED_EVENT_ID
            WHERE te.$ID = :id AND tm.$MOVEMENT_DATE_TIME >= :begin AND tm.$MOVEMENT_DATE_TIME <= :end
        
            UNION
        
            SELECT tp.$ID FROM $PARTICIPANT_TABLE tp
            INNER JOIN $EVENT_TABLE te ON te.$ID = tp.$LINKED_EVENT_ID
            WHERE te.$ID = :id AND (tp.$PARTICIPANT_BEGIN IS NOT NULL OR tp.$PARTICIPANT_BEGIN >= :begin)
                AND (tp.$PARTICIPANT_END IS NOT NULL OR tp.$PARTICIPANT_END >= :end)
        
            UNION
        
            SELECT tg.$ID FROM $GROUP_TABLE tg
            INNER JOIN $EVENT_TABLE te ON te.$ID = tg.$LINKED_EVENT_ID
            WHERE te.$ID = :id AND (tg.$GROUP_BEGIN IS NOT NULL OR tg.$GROUP_BEGIN >= :begin)
                AND (tg.$GROUP_END IS NOT NULL OR tg.$GROUP_END >= :end)
        
            UNION
        
            SELECT tv.$ID FROM $VEHICLE_TABLE tv
            INNER JOIN $EVENT_TABLE te ON te.$ID = tv.$LINKED_EVENT_ID
            WHERE te.$ID = :id AND (tv.$VEHICLE_BEGIN IS NOT NULL OR tv.$VEHICLE_BEGIN >= :begin)
                AND (tv.$VEHICLE_END IS NOT NULL OR tv.$VEHICLE_END >= :end)
        ) AS t
        """
    )
    fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<EventRelationEntity>
}
