package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID

object MovementFields {
    const val MOVEMENT_TABLE = "tb_movement"
    const val MOVEMENT_CONTENT_TABLE = "tb_movement_content"

    const val MOVEMENT_DATE_TIME = "date_time"
    const val MOVEMENT_TYPE = "type"
    const val MOVEMENT_CONTENT = "content"

    private const val PARTICIPANT_PREFIX = "participant_"
    const val MOVEMENT_CONTENT_PARTICIPANT_ID = "$PARTICIPANT_PREFIX$ID"
    const val MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME = "${PARTICIPANT_PREFIX}$PARTICIPANT_FIRST_NAME"
    const val MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME = "${PARTICIPANT_PREFIX}$PARTICIPANT_LAST_NAME"
    const val MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY = "${PARTICIPANT_PREFIX}$PARTICIPANT_BIRTHDAY"

    private const val MOVEMENT_PREFIX = "movement_"
    const val MOVEMENT_CONTENT_MOVEMENT_ID = "$MOVEMENT_PREFIX$ID"
}
