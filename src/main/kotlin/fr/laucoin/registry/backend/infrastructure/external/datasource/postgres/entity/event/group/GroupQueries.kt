package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_MEMBERS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_TABLE
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

object GroupQueries {
    const val SELECT_CONTENT = """
        JSON_AGG(
            JSON_BUILD_OBJECT(
                'id', $PARTICIPANT_TABLE.$ID,
                'firstName', $PARTICIPANT_TABLE.$PARTICIPANT_FIRST_NAME,
                'lastName', $PARTICIPANT_TABLE.$PARTICIPANT_LAST_NAME,
                'birthday', $PARTICIPANT_TABLE.$PARTICIPANT_BIRTHDAY,
                'begin', $PARTICIPANT_TABLE.$PARTICIPANT_BEGIN,
                'end', $PARTICIPANT_TABLE.$PARTICIPANT_END,
                'user', NULL,
                'purged', $PARTICIPANT_TABLE.$PARTICIPANT_PURGED
            )
        ) AS $GROUP_MEMBERS
    """

    const val CONTENT_JOIN = """
        LEFT JOIN $GROUP_CONTENT_TABLE
            ON t.$ID = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID
        INNER JOIN $PARTICIPANT_TABLE
            ON $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID = $PARTICIPANT_TABLE.$ID
            AND $PARTICIPANT_TABLE.$PARTICIPANT_PURGED IS FALSE
            AND $PARTICIPANT_TABLE.$VISIBLE IS TRUE
    """

    const val GROUP_BY_GROUP = """
        t.$ID, t.$GROUP_NAME, t.$GROUP_BEGIN, t.$GROUP_END,
         event_tb.$ID, $LINKED_EVENT_NAME, $LINKED_EVENT_START_TIME, $LINKED_EVENT_END_TIME, $LINKED_EVENT_OPTIONS,
         $CREATOR_FIRST_NAME, $CREATOR_LAST_NAME, $CREATOR_EMAIL,
         $LAST_MODIFIER_FIRST_NAME, $LAST_MODIFIER_LAST_NAME, $LAST_MODIFIER_EMAIL,
        t.$VISIBLE, t.$CREATOR_ID, t.$CREATED_AT, t.$LAST_MODIFIER_ID, t.$LAST_MODIFIER_DATE
    """

    const val IN_DATE_RANGE_CLAUSE = """
        (:startDateTime IS NULL OR :startDateTime <= t.$GROUP_END) AND
        (:endDateTime IS NULL OR :endDateTime >= t.$GROUP_BEGIN)
    """

    const val PRESENT_CLAUSE = """
        (:onlyPresent IS FALSE OR (
            (t.$GROUP_BEGIN IS NULL OR t.$GROUP_BEGIN <= CURRENT_TIMESTAMP)
            AND (t.$GROUP_END IS NULL OR t.$GROUP_END <= CURRENT_TIMESTAMP)
        ))
    """

}
