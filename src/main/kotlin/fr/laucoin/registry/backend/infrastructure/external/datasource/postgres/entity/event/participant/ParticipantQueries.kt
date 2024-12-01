package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TABLE

object ParticipantQueries {
    const val NOT_PURGED_CLAUSE = "t.$PARTICIPANT_PURGED IS FALSE"

    const val IN_DATE_RANGE_CLAUSE = """
        (:startDateTime IS NULL OR :startDateTime <= t.$EVENT_END) AND
        (:endDateTime IS NULL OR :endDateTime >= t.$EVENT_BEGIN)
    """

    private const val LINKED_USER_TABLE = "user_tb"
    const val SELECT_LINKED_USER = """
        $LINKED_USER_TABLE.$USER_FIRST_NAME AS $PARTICIPANT_USER_FIRST_NAME,
        $LINKED_USER_TABLE.$USER_LAST_NAME AS $PARTICIPANT_USER_LAST_NAME,
        $LINKED_USER_TABLE.$USER_EMAIL AS $PARTICIPANT_USER_EMAIL
    """
    const val USER_JOIN =
        "LEFT JOIN $USER_TABLE $LINKED_USER_TABLE ON t.$PARTICIPANT_USER_ID = $LINKED_USER_TABLE.$ID AND $LINKED_USER_TABLE.$VISIBLE IS TRUE"
}
