package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATED_AT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_START_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TABLE

object ParticipantQueries {
    const val SELECT_LINKED_GROUPS = """
        JSON_AGG(
            JSON_BUILD_OBJECT(
                'id', $GROUP_TABLE.$ID,
                'name', $GROUP_TABLE.$GROUP_NAME,
                'begin', $GROUP_TABLE.$GROUP_BEGIN,
                'end', $GROUP_TABLE.$GROUP_END,
                'visible', $GROUP_TABLE.$VISIBLE
            )
        ) AS $PARTICIPANT_GROUPS
    """

    const val GROUPS_JOIN = """
        LEFT JOIN $GROUP_CONTENT_TABLE
            ON t.$ID = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID
        LEFT JOIN $GROUP_TABLE
            ON $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID = $GROUP_TABLE.$ID
            AND $GROUP_TABLE.$VISIBLE IS TRUE
    """

    const val GROUP_BY_PARTICIPANT = """
        t.$ID, t.$PARTICIPANT_FIRST_NAME, t.$PARTICIPANT_LAST_NAME, t.$PARTICIPANT_BIRTHDAY, t.$PARTICIPANT_BEGIN,
        t.$PARTICIPANT_END, t.$PARTICIPANT_PURGED, $PARTICIPANT_USER_FIRST_NAME, $PARTICIPANT_USER_LAST_NAME, $PARTICIPANT_USER_EMAIL,
         event_tb.$ID, $LINKED_EVENT_NAME, $LINKED_EVENT_START_TIME, $LINKED_EVENT_END_TIME, $LINKED_EVENT_OPTIONS,
         $CREATOR_FIRST_NAME, $CREATOR_LAST_NAME, $CREATOR_EMAIL,
         $LAST_MODIFIER_FIRST_NAME, $LAST_MODIFIER_LAST_NAME, $LAST_MODIFIER_EMAIL,
        t.$VISIBLE, t.$CREATOR_ID, t.$CREATED_AT, t.$LAST_MODIFIER_ID, t.$LAST_MODIFIER_DATE
    """

    const val NOT_PURGED_CLAUSE = "t.$PARTICIPANT_PURGED IS FALSE"

    const val IN_DATE_RANGE_CLAUSE = """
        (:startDateTime IS NULL OR :startDateTime <= t.$EVENT_END) AND
        (:endDateTime IS NULL OR :endDateTime >= t.$EVENT_BEGIN)
    """

    // TODO: Add a clause to consider the less convenient group of participants
    private const val LINKED_GROUP_TABLE = "group_tb"
    private const val PARTICIPANT_PREFIX = "participant_"
    private const val GROUP_PREFIX = "group_"
    const val PRESENT_CLAUSE = """
        (:onlyPresent IS FALSE OR (
            (
                (t.$PARTICIPANT_BEGIN IS NULL AND (
                    $LINKED_GROUP_TABLE.$GROUP_PREFIX$GROUP_BEGIN IS NULL OR $LINKED_GROUP_TABLE.$GROUP_PREFIX$GROUP_BEGIN <= CURRENT_TIMESTAMP
                )) OR t.$PARTICIPANT_BEGIN <= CURRENT_TIMESTAMP
            )
            AND (
                (t.$PARTICIPANT_END IS NULL AND (
                    $LINKED_GROUP_TABLE.$GROUP_PREFIX$GROUP_END IS NULL OR $LINKED_GROUP_TABLE.$GROUP_PREFIX$GROUP_END <= CURRENT_TIMESTAMP
                )) OR t.$PARTICIPANT_END <= CURRENT_TIMESTAMP
            )
        ))
    """
    const val GROUP_JOIN = """
        LEFT JOIN (
            SELECT gc.$PARTICIPANT_PREFIX$ID,
            CASE
                WHEN COUNT(CASE WHEN g.$GROUP_BEGIN IS NULL THEN 1 END) > 0 THEN NULL
                ELSE MIN(g.$GROUP_BEGIN)
            END AS $GROUP_PREFIX$GROUP_BEGIN,
            CASE
                WHEN COUNT(CASE WHEN g.$GROUP_END IS NULL THEN 1 END) > 0 THEN NULL
                ELSE MAX(g.$GROUP_END)
            END AS $GROUP_PREFIX$GROUP_END
            FROM $GROUP_CONTENT_TABLE gc
            INNER JOIN $GROUP_TABLE g ON g.$ID = gc.$GROUP_PREFIX$ID
            WHERE g.$VISIBLE IS TRUE
            GROUP BY gc.$PARTICIPANT_PREFIX$ID
        ) $LINKED_GROUP_TABLE ON t.$ID = $LINKED_GROUP_TABLE.$PARTICIPANT_PREFIX$ID
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
