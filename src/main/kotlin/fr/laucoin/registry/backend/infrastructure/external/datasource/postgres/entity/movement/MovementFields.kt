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
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LICENSE_PLATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_MODEL

object MovementFields {
    const val MOVEMENT_TABLE = "tb_movement"
    const val MOVEMENT_CONTENT_TABLE = "tb_movement_content"

    const val MOVEMENT_DATE_TIME = "date_time"
    const val MOVEMENT_TYPE = "type"
    const val MOVEMENT_REASON = "reason"

    private const val MOVEMENT_ACTIVITY_PREFIX = "activity_"
    const val MOVEMENT_ACTIVITY_ID = "$MOVEMENT_ACTIVITY_PREFIX$ID"
    const val MOVEMENT_ACTIVITY_NAME = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_NAME"
    const val MOVEMENT_ACTIVITY_DESCRIPTION = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_DESCRIPTION"
    const val MOVEMENT_ACTIVITY_DURATION = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_DURATION"
    const val MOVEMENT_ACTIVITY_MIN_ALLOWED_PARTICIPANTS = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_MIN_ALLOWED_PARTICIPANTS"
    const val MOVEMENT_ACTIVITY_MAX_ALLOWED_PARTICIPANTS = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_MAX_ALLOWED_PARTICIPANTS"
    const val MOVEMENT_ACTIVITY_START_AVAILABILITY_DATE = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_START_AVAILABILITY_DATE"
    const val MOVEMENT_ACTIVITY_START_AVAILABILITY_TIME = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_START_AVAILABILITY_TIME"
    const val MOVEMENT_ACTIVITY_END_AVAILABILITY_DATE = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_END_AVAILABILITY_DATE"
    const val MOVEMENT_ACTIVITY_END_AVAILABILITY_TIME = "$MOVEMENT_ACTIVITY_PREFIX$ACTIVITY_END_AVAILABILITY_TIME"

    const val MOVEMENT_CONTENT_POOL_NAME = "pool_name"

    private const val PARTICIPANT_PREFIX = "participant_"
    const val MOVEMENT_CONTENT_PARTICIPANT_ID = "$PARTICIPANT_PREFIX$ID"
    const val MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME = "$PARTICIPANT_PREFIX$PARTICIPANT_FIRST_NAME"
    const val MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME = "$PARTICIPANT_PREFIX$PARTICIPANT_LAST_NAME"
    const val MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY = "$PARTICIPANT_PREFIX$PARTICIPANT_BIRTHDAY"
    const val MOVEMENT_CONTENT_PARTICIPANT_TYPE = "$PARTICIPANT_PREFIX$PARTICIPANT_TYPE"

    private const val VEHICLE_PREFIX = "vehicle_"
    const val MOVEMENT_CONTENT_VEHICLE_ID = "$VEHICLE_PREFIX$ID"
    const val MOVEMENT_CONTENT_VEHICLE_LICENSE_PLATE = "$VEHICLE_PREFIX$VEHICLE_LICENSE_PLATE"
    const val MOVEMENT_CONTENT_VEHICLE_BRAND = "$VEHICLE_PREFIX$VEHICLE_BRAND"
    const val MOVEMENT_CONTENT_VEHICLE_MODEL = "$VEHICLE_PREFIX$VEHICLE_MODEL"

    private const val MOVEMENT_PREFIX = "movement_"
    const val MOVEMENT_CONTENT_MOVEMENT_ID = "$MOVEMENT_PREFIX$ID"
}
