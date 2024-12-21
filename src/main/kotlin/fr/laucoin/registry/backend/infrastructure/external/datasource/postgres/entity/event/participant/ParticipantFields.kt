package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant

object ParticipantFields {
    const val PARTICIPANT_TABLE = "tb_participant"

    const val PARTICIPANT_FIRST_NAME = "first_name"
    const val PARTICIPANT_LAST_NAME = "last_name"
    const val PARTICIPANT_BIRTHDAY = "birthday"
    const val PARTICIPANT_GROUPS = "groups"
    const val PARTICIPANT_BEGIN = "begin"
    const val PARTICIPANT_END = "finish"
    const val PARTICIPANT_USER_ID = "user_id"
    const val PARTICIPANT_USER_FIRST_NAME = "user_first_name"
    const val PARTICIPANT_USER_LAST_NAME = "user_last_name"
    const val PARTICIPANT_USER_EMAIL = "user_email"
    const val PARTICIPANT_PURGED = "purged"
}
