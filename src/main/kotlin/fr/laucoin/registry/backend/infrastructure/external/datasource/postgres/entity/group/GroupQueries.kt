package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group

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
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_START_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_START_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_MEMBERS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE

object GroupQueries {
    const val SELECT_CONTENT_TO_CONTENT = """
        $PARTICIPANT_TABLE.$PARTICIPANT_FIRST_NAME as $MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME,
        $PARTICIPANT_TABLE.$PARTICIPANT_LAST_NAME as $MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME,
        $PARTICIPANT_TABLE.$PARTICIPANT_BIRTHDAY as $MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY
    """

    const val CONTENT_TO_CONTENT_JOIN = """
        INNER JOIN $PARTICIPANT_TABLE
            ON t.$GROUP_CONTENT_PARTICIPANT_ID = $PARTICIPANT_TABLE.$ID
            AND $PARTICIPANT_TABLE.$PARTICIPANT_PURGED IS FALSE
    """

    const val SELECT_CONTENT = """
        JSON_AGG(
            JSON_BUILD_OBJECT(
                'id', $PARTICIPANT_TABLE.$ID,
                'firstName', $PARTICIPANT_TABLE.$PARTICIPANT_FIRST_NAME,
                'lastName', $PARTICIPANT_TABLE.$PARTICIPANT_LAST_NAME,
                'birthday', $PARTICIPANT_TABLE.$PARTICIPANT_BIRTHDAY,
                'begin', JSON_BUILD_OBJECT(
                    'date', $PARTICIPANT_TABLE.$PARTICIPANT_START_AVAILABILITY_DATE,
                    'time', $PARTICIPANT_TABLE.$PARTICIPANT_START_AVAILABILITY_TIME
                ),
                'end', JSON_BUILD_OBJECT(
                    'date', $PARTICIPANT_TABLE.$PARTICIPANT_END_AVAILABILITY_DATE,
                    'time', $PARTICIPANT_TABLE.$PARTICIPANT_END_AVAILABILITY_TIME
                ),
                'user', NULL,
                'purged', $PARTICIPANT_TABLE.$PARTICIPANT_PURGED
            )
        ) AS $GROUP_MEMBERS
    """

    const val CONTENT_JOIN = """
        LEFT JOIN $GROUP_CONTENT_TABLE
            ON t.$ID = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID
        LEFT JOIN $PARTICIPANT_TABLE
            ON $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID = $PARTICIPANT_TABLE.$ID
            AND $PARTICIPANT_TABLE.$PARTICIPANT_PURGED IS FALSE
            AND $PARTICIPANT_TABLE.$VISIBLE IS TRUE
    """

    const val GROUP_BY_GROUP = """
         t.$ID, t.$GROUP_NAME, t.$GROUP_START_AVAILABILITY_DATE, t.$GROUP_START_AVAILABILITY_TIME, t.$GROUP_END_AVAILABILITY_DATE, t.$GROUP_END_AVAILABILITY_TIME,
         event_tb.$ID, $LINKED_EVENT_NAME, $LINKED_EVENT_START_DATE, $LINKED_EVENT_START_TIME, $LINKED_EVENT_END_DATE, $LINKED_EVENT_END_TIME, $LINKED_EVENT_OPTIONS,
         $CREATOR_FIRST_NAME, $CREATOR_LAST_NAME, $CREATOR_EMAIL,
         $LAST_MODIFIER_FIRST_NAME, $LAST_MODIFIER_LAST_NAME, $LAST_MODIFIER_EMAIL,
        t.$VISIBLE, t.$CREATOR_ID, t.$CREATED_AT, t.$LAST_MODIFIER_ID, t.$LAST_MODIFIER_DATE
    """

    const val GROUP_TEXT_SEARCH_CLAUSE = """
        (
            :textSearched IS NULL OR UNACCENT(t.$GROUP_NAME) ILIKE '%' || UNACCENT(:textSearched) || '%'
        )
    """
    const val GROUP_PRESENCE_CLAUSE = """
        (
            :presenceSearched IS NULL OR :presenceSearched = (
                (
                    COALESCE(t.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) < CURRENT_DATE
                    OR (COALESCE(t.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                ) AND
                (
                    COALESCE(t.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) > CURRENT_DATE
                    OR (COALESCE(t.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                )
            )
        )
    """

    const val DATE_IN_GROUP_DATES_RANGE_CLAUSE = """
        (
            :dateTimeSearched IS NULL OR (
                (
                    COALESCE(t.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) < CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CAST(:dateTimeSearched AS TIME))
                ) AND
                (
                    COALESCE(t.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) > CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CAST(:dateTimeSearched AS TIME))
                )
            )
        )
    """
}
