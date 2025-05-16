package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_INSIDE_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_OUTSIDE_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TYPE

object GroupQueries {
    const val WITH_PARTICIPANT_GROUPS = """
        participants_groups AS (
            SELECT t.$ID, t.$PARTICIPANT_START_AVAILABILITY_DATE, t.$PARTICIPANT_START_AVAILABILITY_TIME, t.$PARTICIPANT_END_AVAILABILITY_DATE, t.$PARTICIPANT_END_AVAILABILITY_TIME,
                JSON_AGG($GROUP_TABLE.$ID) FILTER (WHERE $GROUP_TABLE.$ID IS NOT NULL) as participant_groups,
                JSON_AGG(group_presence.$ID) FILTER (WHERE group_presence.$ID IS NOT NULL) as participant_groups_available
            FROM $PARTICIPANT_TABLE t
            LEFT JOIN $GROUP_CONTENT_TABLE ON $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID = t.$ID
            LEFT JOIN $GROUP_TABLE ON $GROUP_TABLE.$ID = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID AND $GROUP_TABLE.$VISIBLE IS TRUE
            LEFT JOIN $GROUP_TABLE group_presence ON group_presence.$ID = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID AND group_presence.$VISIBLE IS TRUE
                AND (
                    (
                        COALESCE(group_presence.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) < CURRENT_DATE
                        OR (COALESCE(group_presence.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(group_presence.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                    ) AND (
                        COALESCE(group_presence.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) > CURRENT_DATE
                        OR (COALESCE(group_presence.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(group_presence.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                    )
                )
            WHERE t.$LINKED_PROJECT_ID = :projectId AND t.$VISIBLE IS TRUE
            GROUP BY t.$ID, t.$PARTICIPANT_START_AVAILABILITY_DATE, t.$PARTICIPANT_START_AVAILABILITY_TIME, t.$PARTICIPANT_END_AVAILABILITY_DATE, t.$PARTICIPANT_END_AVAILABILITY_TIME
        )
    """

    const val WITH_GROUP_INSIDE_MEMBERS = """
        inside_members AS (
            SELECT $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID, COUNT(t.$ID) as $GROUP_INSIDE_MEMBERS_COUNT
            FROM $MOVEMENT_TABLE t
            INNER JOIN (
                SELECT MAX(t.$MOVEMENT_DATE_TIME) as $PARTICIPANT_LAST_MOVEMENT_DATE_TIME, $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_PARTICIPANT_ID
                FROM $MOVEMENT_TABLE t
                INNER JOIN $MOVEMENT_CONTENT_TABLE ON $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
                WHERE t.$VISIBLE IS TRUE
                GROUP BY $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_PARTICIPANT_ID
            ) AS plm ON plm.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME = t.$MOVEMENT_DATE_TIME
            INNER JOIN $GROUP_CONTENT_TABLE ON plm.$MOVEMENT_CONTENT_PARTICIPANT_ID = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID
            INNER JOIN participants_groups ON plm.$MOVEMENT_CONTENT_PARTICIPANT_ID = participants_groups.$ID
            WHERE t.$MOVEMENT_TYPE = 'IN' AND (
                (
                    (
                        participants_groups.$PARTICIPANT_START_AVAILABILITY_DATE IS NULL AND (
                            participants_groups.participant_groups IS NULL OR json_array_length(participants_groups.participant_groups) = 0
                            OR (participants_groups.participant_groups_available IS NOT NULL AND json_array_length(participants_groups.participant_groups_available) > 0)
                        )
                    ) OR
                    COALESCE(participants_groups.$PARTICIPANT_START_AVAILABILITY_DATE, '+infinity'::DATE) < CURRENT_DATE
                    OR (COALESCE(participants_groups.$PARTICIPANT_START_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(participants_groups.$PARTICIPANT_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                ) AND
                (
                    (
                        participants_groups.$PARTICIPANT_END_AVAILABILITY_DATE IS NULL AND (
                            participants_groups.participant_groups IS NULL OR json_array_length(participants_groups.participant_groups) = 0
                            OR (participants_groups.participant_groups_available IS NOT NULL AND json_array_length(participants_groups.participant_groups_available) > 0)
                        )
                    ) OR
                    COALESCE(participants_groups.$PARTICIPANT_END_AVAILABILITY_DATE, '-infinity'::DATE) > CURRENT_DATE
                    OR (COALESCE(participants_groups.$PARTICIPANT_END_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(participants_groups.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                )
            )
            GROUP BY $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID
        )
    """

    const val SELECT_MEMBERS_COUNTS = """
        members.$GROUP_MEMBERS_COUNT,
        CASE WHEN inside_members.$GROUP_INSIDE_MEMBERS_COUNT IS NULL THEN 0 ELSE inside_members.$GROUP_INSIDE_MEMBERS_COUNT END as $GROUP_INSIDE_MEMBERS_COUNT,
        members.$GROUP_MEMBERS_COUNT - CASE WHEN inside_members.$GROUP_INSIDE_MEMBERS_COUNT IS NULL THEN 0 ELSE inside_members.$GROUP_INSIDE_MEMBERS_COUNT END as $GROUP_OUTSIDE_MEMBERS_COUNT
    """

    const val GROUP_INSIDE_MEMBERS_JOIN = """
        LEFT JOIN inside_members ON inside_members.$GROUP_CONTENT_GROUP_ID = t.$ID
    """


    const val WITH_GROUP_MEMBERS = """
        members AS (
            SELECT t.$GROUP_CONTENT_GROUP_ID, COUNT(t.$GROUP_CONTENT_PARTICIPANT_ID) as $GROUP_MEMBERS_COUNT
            FROM $GROUP_CONTENT_TABLE t
            INNER JOIN $PARTICIPANT_TABLE ON t.$GROUP_CONTENT_PARTICIPANT_ID = $PARTICIPANT_TABLE.$ID
            WHERE $PARTICIPANT_TABLE.$VISIBLE IS TRUE
            GROUP BY t.$GROUP_CONTENT_GROUP_ID
        )
    """

    const val GROUP_MEMBERS_JOIN = """
        INNER JOIN members ON members.$GROUP_CONTENT_GROUP_ID = t.$ID
    """

    const val SELECT_CONTENT_TO_CONTENT = """
        $PARTICIPANT_TABLE.$PARTICIPANT_FIRST_NAME as $GROUP_CONTENT_PARTICIPANT_FIRST_NAME,
        $PARTICIPANT_TABLE.$PARTICIPANT_LAST_NAME as $GROUP_CONTENT_PARTICIPANT_LAST_NAME,
        $PARTICIPANT_TABLE.$PARTICIPANT_BIRTHDAY as $GROUP_CONTENT_PARTICIPANT_BIRTHDAY,
        $PARTICIPANT_TABLE.$PARTICIPANT_TYPE as $GROUP_CONTENT_PARTICIPANT_TYPE
    """

    const val CONTENT_TO_CONTENT_JOIN = """
        INNER JOIN $PARTICIPANT_TABLE
            ON t.$GROUP_CONTENT_PARTICIPANT_ID = $PARTICIPANT_TABLE.$ID
            AND $PARTICIPANT_TABLE.$PARTICIPANT_PURGED IS FALSE
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
