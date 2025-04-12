package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_LICENSE_PLATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LICENSE_PLATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE

object MovementQueries {
    const val SELECT_CONTENT = """
        $PARTICIPANT_TABLE.$PARTICIPANT_FIRST_NAME as $MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME,
        $PARTICIPANT_TABLE.$PARTICIPANT_LAST_NAME as $MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME,
        $PARTICIPANT_TABLE.$PARTICIPANT_BIRTHDAY as $MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY,
        $VEHICLE_TABLE.$VEHICLE_LICENSE_PLATE as $MOVEMENT_CONTENT_VEHICLE_LICENSE_PLATE,
        $VEHICLE_TABLE.$VEHICLE_BRAND as $MOVEMENT_CONTENT_VEHICLE_BRAND,
        $VEHICLE_TABLE.$VEHICLE_MODEL as $MOVEMENT_CONTENT_VEHICLE_MODEL
    """

    const val CONTENT_JOIN = """
        INNER JOIN $PARTICIPANT_TABLE
            ON t.$MOVEMENT_CONTENT_PARTICIPANT_ID = $PARTICIPANT_TABLE.$ID
            AND $PARTICIPANT_TABLE.$PARTICIPANT_PURGED IS FALSE
        LEFT JOIN $VEHICLE_TABLE
            ON t.$MOVEMENT_CONTENT_VEHICLE_ID = $VEHICLE_TABLE.$ID
    """

    private const val LINKED_ACTIVITY_TABLE = "activity_tb"
    const val SELECT_LINKED_ACTIVITY = """
        $LINKED_ACTIVITY_TABLE.$ID AS $MOVEMENT_ACTIVITY_ID,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_NAME AS $MOVEMENT_ACTIVITY_NAME,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_DESCRIPTION AS $MOVEMENT_ACTIVITY_DESCRIPTION,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_DURATION AS $MOVEMENT_ACTIVITY_DURATION,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_MIN_ALLOWED_PARTICIPANTS AS $MOVEMENT_ACTIVITY_MIN_ALLOWED_PARTICIPANTS,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_MAX_ALLOWED_PARTICIPANTS AS $MOVEMENT_ACTIVITY_MAX_ALLOWED_PARTICIPANTS,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_DATE AS $MOVEMENT_ACTIVITY_START_AVAILABILITY_DATE,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_TIME AS $MOVEMENT_ACTIVITY_START_AVAILABILITY_TIME,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_DATE AS $MOVEMENT_ACTIVITY_END_AVAILABILITY_DATE,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_TIME AS $MOVEMENT_ACTIVITY_END_AVAILABILITY_TIME
    """
    const val ACTIVITY_JOIN =
        "LEFT JOIN $ACTIVITY_TABLE $LINKED_ACTIVITY_TABLE ON t.$MOVEMENT_ACTIVITY_ID = $LINKED_ACTIVITY_TABLE.$ID"

    const val MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE = """
        (
            COALESCE(:startDateTimeSearched, '-infinity'::TIMESTAMP) <= t.$MOVEMENT_DATE_TIME AND
            COALESCE(:endDateTimeSearched, '+infinity'::TIMESTAMP) >= t.$MOVEMENT_DATE_TIME
        )
    """

    const val MOVEMENT_TYPE_CLAUSE = """
        (t.$MOVEMENT_TYPE IN (:typeSearched))
    """
}
