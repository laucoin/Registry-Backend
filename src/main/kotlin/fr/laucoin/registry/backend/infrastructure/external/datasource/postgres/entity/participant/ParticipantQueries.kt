package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_AVAILABLE_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TABLE

object ParticipantQueries {
    private const val PARTICIPANT_PREFIX = "participant_"
    const val WITH_PARTICIPANT_LAST_MOVEMENT = """
        last_movement AS (
            SELECT plm.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME, plm.$PARTICIPANT_PREFIX$ID, t.$MOVEMENT_TYPE
            FROM $MOVEMENT_TABLE t
            INNER JOIN (
                SELECT MAX(t.$MOVEMENT_DATE_TIME) as $PARTICIPANT_LAST_MOVEMENT_DATE_TIME, $MOVEMENT_CONTENT_TABLE.$PARTICIPANT_PREFIX$ID
                FROM $MOVEMENT_TABLE t
                INNER JOIN $MOVEMENT_CONTENT_TABLE ON $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
                WHERE t.$VISIBLE IS TRUE
                GROUP BY $MOVEMENT_CONTENT_TABLE.$PARTICIPANT_PREFIX$ID
            ) AS plm ON plm.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME = t.$MOVEMENT_DATE_TIME
        )
    """

    const val SELECT_LAST_MOVEMENT = """
        last_movement.type AS $PARTICIPANT_LAST_MOVEMENT_TYPE,
        last_movement.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME
    """

    const val LAST_MOVEMENT_JOIN = """
        LEFT JOIN last_movement ON last_movement.$PARTICIPANT_PREFIX$ID = t.$ID
    """

    private const val GROUP_PREFIX = "group_"
    const val WITH_PARTICIPANT_GROUPS = """
        filtered_groups AS (
            SELECT t.$ID as $PARTICIPANT_PREFIX$ID,
            JSON_AGG(
                JSON_BUILD_OBJECT(
                    'id', $GROUP_TABLE.$ID,
                    'name', $GROUP_TABLE.$GROUP_NAME
                )
            ) FILTER (WHERE $GROUP_TABLE.$ID IS NOT NULL) as $PARTICIPANT_GROUPS,
            MIN(
                CASE
                    WHEN group_presence.$GROUP_START_AVAILABILITY_DATE IS NULL THEN
                      '-infinity'::timestamptz
                    ELSE
                      (group_presence.$GROUP_START_AVAILABILITY_DATE + COALESCE(group_presence.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::time))::timestamptz
                  END
            ) AS min_start_availability,
            MAX(
                CASE
                    WHEN group_presence.$GROUP_END_AVAILABILITY_DATE IS NULL THEN
                      '+infinity'::timestamptz
                    ELSE
                      (group_presence.$GROUP_END_AVAILABILITY_DATE + COALESCE(group_presence.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::time))::timestamptz
                  END
            ) AS max_end_availability,
            JSON_AGG(group_presence.$ID) FILTER (WHERE group_presence.$ID IS NOT NULL) as $PARTICIPANT_AVAILABLE_GROUPS,
            JSON_AGG(group_presence_on_date.$ID) FILTER (WHERE group_presence_on_date.$ID IS NOT NULL) as available_groups_on_date
            FROM $PARTICIPANT_TABLE t
            LEFT JOIN $GROUP_CONTENT_TABLE ON $GROUP_CONTENT_TABLE.$PARTICIPANT_PREFIX$ID = t.$ID
            LEFT JOIN $GROUP_TABLE ON $GROUP_TABLE.$ID = $GROUP_CONTENT_TABLE.$GROUP_PREFIX$ID AND $GROUP_TABLE.$VISIBLE IS TRUE
            LEFT JOIN $GROUP_TABLE group_presence ON group_presence.$ID = $GROUP_CONTENT_TABLE.$GROUP_PREFIX$ID AND group_presence.$VISIBLE IS TRUE
                AND (
                    (
                        COALESCE(group_presence.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) < CURRENT_DATE
                        OR (COALESCE(group_presence.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(group_presence.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                    ) AND (
                        COALESCE(group_presence.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) > CURRENT_DATE
                        OR (COALESCE(group_presence.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(group_presence.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                    )
                )
            LEFT JOIN $GROUP_TABLE group_presence_on_date ON group_presence_on_date.$ID = $GROUP_CONTENT_TABLE.$GROUP_PREFIX$ID AND group_presence_on_date.$VISIBLE IS TRUE
                AND :dateTimeSearched IS NULL OR (
                    (
                        COALESCE(group_presence_on_date.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) < CAST(:dateTimeSearched AS DATE)
                        OR (COALESCE(group_presence_on_date.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(group_presence_on_date.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CAST(:dateTimeSearched AS TIME))
                    ) AND (
                        COALESCE(group_presence_on_date.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) > CAST(:dateTimeSearched AS DATE)
                        OR (COALESCE(group_presence_on_date.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(group_presence_on_date.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CAST(:dateTimeSearched AS TIME))
                    )
                )
            WHERE t.$LINKED_PROJECT_ID = :projectId
            GROUP BY t.$ID
        )
    """

    const val SELECT_LINKED_GROUPS = "fg.$PARTICIPANT_GROUPS, fg.$PARTICIPANT_AVAILABLE_GROUPS"

    const val GROUPS_JOIN = """
        LEFT JOIN filtered_groups fg ON t.$ID = fg.$PARTICIPANT_PREFIX$ID
    """

    private const val LINKED_USER_TABLE = "user_tb"
    const val SELECT_LINKED_USER = """
        $LINKED_USER_TABLE.$USER_FIRST_NAME AS $PARTICIPANT_USER_FIRST_NAME,
        $LINKED_USER_TABLE.$USER_LAST_NAME AS $PARTICIPANT_USER_LAST_NAME,
        $LINKED_USER_TABLE.$USER_EMAIL AS $PARTICIPANT_USER_EMAIL
    """
    const val USER_JOIN = """
        LEFT JOIN $USER_TABLE $LINKED_USER_TABLE ON t.$PARTICIPANT_USER_ID = $LINKED_USER_TABLE.$ID
        AND $LINKED_USER_TABLE.$PARTICIPANT_PURGED IS FALSE
        AND $LINKED_USER_TABLE.$VISIBLE IS TRUE
    """

    const val NOT_PURGED_CLAUSE = "t.$PARTICIPANT_PURGED IS FALSE"

    const val SELECT_PARTICIPANT_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity(t.search_text, :textSearched)
        END AS similarity_score
    """

    const val PARTICIPANT_TEXT_SEARCH_CLAUSE = "(:textSearched IS NULL OR similarity(t.search_text, :textSearched) > 0)"

    const val PARTICIPANT_MAJOR_CLAUSE =
        "(:isMajor IS NULL OR :isMajor = (t.$PARTICIPANT_BIRTHDAY >= CURRENT_DATE - INTERVAL '18 years'))"

    const val PARTICIPANT_TYPE_SEARCH_CLAUSE = "(:typeSearched IS NULL OR t.type = :typeSearched)"

    const val PARTICIPANT_AVAILABILITY_CLAUSE = """
        (
            :availabilitySearched IS NULL OR :availabilitySearched = (
                (
                    COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, CAST(fg.min_start_availability AS DATE), '+infinity'::DATE) < CURRENT_DATE
                    OR (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, CAST(fg.min_start_availability AS DATE), '+infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, CAST(fg.min_start_availability AS TIME), '00:00:00.000000'::TIME) <= CURRENT_TIME)
                ) AND
                (
                    COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, CAST(fg.max_end_availability AS DATE), '-infinity'::DATE) > CURRENT_DATE
                    OR (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, CAST(fg.max_end_availability AS DATE), '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, CAST(fg.max_end_availability AS TIME), '23:59:59.999999'::TIME) >= CURRENT_TIME)
                )
            )
        )
    """

    const val PARTICIPANT_PRESENCE_CLAUSE =
        "(:presenceSearched IS NULL OR :presenceSearched != (last_movement.type IS NULL OR last_movement.type = 'OUT'))"

    const val DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE = """
        (
            :dateTimeSearched IS NULL OR (
                (
                    fg.$PARTICIPANT_GROUPS IS NULL OR json_array_length(fg.$PARTICIPANT_GROUPS) = 0
                    OR (fg.available_groups_on_date IS NOT NULL AND json_array_length(fg.available_groups_on_date) > 0)
                ) AND
                (
                    COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) < CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CAST(:dateTimeSearched AS TIME))
                ) AND
                (
                    COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) > CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CAST(:dateTimeSearched AS TIME))
                )
            )
        )
    """
}
