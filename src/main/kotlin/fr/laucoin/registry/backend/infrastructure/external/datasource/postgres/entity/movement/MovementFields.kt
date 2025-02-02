package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_REGISTRATION

object MovementFields {
    const val MOVEMENT_TABLE = "tb_movement"
    const val MOVEMENT_CONTENT_TABLE = "tb_movement_content"

    const val MOVEMENT_DATE_TIME = "date_time"
    const val MOVEMENT_TYPE = "type"
    const val MOVEMENT_CONTENT = "content"

    const val MOVEMENT_CONTENT_POOL_NAME = "pool_name"

    private const val PARTICIPANT_PREFIX = "participant_"
    const val MOVEMENT_CONTENT_PARTICIPANT_ID = "$PARTICIPANT_PREFIX$ID"
    const val MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME = "$PARTICIPANT_PREFIX$PARTICIPANT_FIRST_NAME"
    const val MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME = "$PARTICIPANT_PREFIX$PARTICIPANT_LAST_NAME"
    const val MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY = "$PARTICIPANT_PREFIX$PARTICIPANT_BIRTHDAY"

    private const val VEHICLE_PREFIX = "vehicle_"
    const val MOVEMENT_CONTENT_VEHICLE_ID = "$VEHICLE_PREFIX$ID"
    const val MOVEMENT_CONTENT_VEHICLE_REGISTRATION = "$VEHICLE_PREFIX$VEHICLE_REGISTRATION"
    const val MOVEMENT_CONTENT_VEHICLE_BRAND = "$VEHICLE_PREFIX$VEHICLE_BRAND"
    const val MOVEMENT_CONTENT_VEHICLE_MODEL = "$VEHICLE_PREFIX$VEHICLE_MODEL"

    private const val MOVEMENT_PREFIX = "movement_"
    const val MOVEMENT_CONTENT_MOVEMENT_ID = "$MOVEMENT_PREFIX$ID"
}
