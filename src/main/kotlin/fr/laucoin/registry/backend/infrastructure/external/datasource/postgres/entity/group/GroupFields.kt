package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME

object GroupFields {
    const val GROUP_TABLE = "tb_group"
    const val GROUP_CONTENT_TABLE = "tb_group_content"

    const val GROUP_NAME = "name"
    const val GROUP_BEGIN = "begin"
    const val GROUP_END = "finish"
    const val GROUP_MEMBERS = "members"

    private const val PARTICIPANT_PREFIX = "participant_"
    const val GROUP_CONTENT_PARTICIPANT_ID = "$PARTICIPANT_PREFIX$ID"
    const val GROUP_CONTENT_PARTICIPANT_FIRST_NAME = "$PARTICIPANT_PREFIX$PARTICIPANT_FIRST_NAME"
    const val GROUP_CONTENT_PARTICIPANT_LAST_NAME = "$PARTICIPANT_PREFIX$PARTICIPANT_LAST_NAME"
    const val GROUP_CONTENT_PARTICIPANT_BIRTHDAY = "$PARTICIPANT_PREFIX$PARTICIPANT_BIRTHDAY"

    private const val GROUP_PREFIX = "group_"
    const val GROUP_CONTENT_GROUP_ID = "$GROUP_PREFIX$ID"
}
