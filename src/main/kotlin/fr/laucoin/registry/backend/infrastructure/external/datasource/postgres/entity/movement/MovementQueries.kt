package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
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
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_START_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_START_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_LICENSE_PLATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_REASON
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
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LICENSE_PLATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LINKED_PROJECT_TABLE

object MovementQueries {
    const val SELECT_CONTENT = """
        $PARTICIPANT_TABLE.$PARTICIPANT_FIRST_NAME as $MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME,
        $PARTICIPANT_TABLE.$PARTICIPANT_LAST_NAME as $MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME,
        $PARTICIPANT_TABLE.$PARTICIPANT_BIRTHDAY as $MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY,
        $PARTICIPANT_TABLE.$PARTICIPANT_TYPE as $MOVEMENT_CONTENT_PARTICIPANT_TYPE,
        $VEHICLE_TABLE.$VEHICLE_LICENSE_PLATE as $MOVEMENT_CONTENT_VEHICLE_LICENSE_PLATE,
        $VEHICLE_TABLE.$VEHICLE_BRAND as $MOVEMENT_CONTENT_VEHICLE_BRAND,
        $VEHICLE_TABLE.$VEHICLE_MODEL as $MOVEMENT_CONTENT_VEHICLE_MODEL
    """

    const val CONTENT_JOIN = """
        INNER JOIN $PARTICIPANT_TABLE
            ON t.$MOVEMENT_CONTENT_PARTICIPANT_ID = $PARTICIPANT_TABLE.$ID
            AND $PARTICIPANT_TABLE.$PARTICIPANT_PURGED IS FALSE
        LEFT JOIN $VEHICLE_TABLE
            ON t.$MOVEMENT_CONTENT_VEHICLE_ID = $VEHICLE_TABLE.$ID
    """

    private const val LINKED_ACTIVITY_TABLE = "activity_tb"
    const val SELECT_LINKED_ACTIVITY = """
        $LINKED_ACTIVITY_TABLE.$ID AS $MOVEMENT_ACTIVITY_ID,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_NAME AS $MOVEMENT_ACTIVITY_NAME,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_DESCRIPTION AS $MOVEMENT_ACTIVITY_DESCRIPTION,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_DURATION AS $MOVEMENT_ACTIVITY_DURATION,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_MIN_ALLOWED_PARTICIPANTS AS $MOVEMENT_ACTIVITY_MIN_ALLOWED_PARTICIPANTS,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_MAX_ALLOWED_PARTICIPANTS AS $MOVEMENT_ACTIVITY_MAX_ALLOWED_PARTICIPANTS,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_DATE AS $MOVEMENT_ACTIVITY_START_AVAILABILITY_DATE,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_TIME AS $MOVEMENT_ACTIVITY_START_AVAILABILITY_TIME,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_DATE AS $MOVEMENT_ACTIVITY_END_AVAILABILITY_DATE,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_TIME AS $MOVEMENT_ACTIVITY_END_AVAILABILITY_TIME
    """
    const val ACTIVITY_JOIN =
        "LEFT JOIN $ACTIVITY_TABLE $LINKED_ACTIVITY_TABLE ON t.$MOVEMENT_ACTIVITY_ID = $LINKED_ACTIVITY_TABLE.$ID"

    const val MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE = """
        (
            COALESCE(:startDateTimeSearched, '-infinity'::TIMESTAMP) <= t.$MOVEMENT_DATE_TIME AND
            COALESCE(:endDateTimeSearched, '+infinity'::TIMESTAMP) >= t.$MOVEMENT_DATE_TIME
        )
    """

    const val MOVEMENT_TYPE_CLAUSE = """
        (t.$MOVEMENT_TYPE IN (:typeSearched))
    """

    const val MOVEMENT_ACTIVITY_CLAUSE = """
        (:linkedToActivity IS NULL OR :linkedToActivity = (t.$MOVEMENT_ACTIVITY_ID IS NOT NULL))
    """

    private const val PARTICIPANT_PREFIX = "participant_"
    const val LAST_PARTICIPANT_MOVEMENT_JOIN = """
        INNER JOIN (
            SELECT MAX(t.$MOVEMENT_DATE_TIME) as $PARTICIPANT_LAST_MOVEMENT_DATE_TIME, $MOVEMENT_CONTENT_TABLE.$PARTICIPANT_PREFIX$ID
            FROM $MOVEMENT_TABLE t
            INNER JOIN $MOVEMENT_CONTENT_TABLE ON $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
            WHERE t.$VISIBLE IS TRUE
            GROUP BY $MOVEMENT_CONTENT_TABLE.$PARTICIPANT_PREFIX$ID
        ) AS plm ON plm.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME = t.$MOVEMENT_DATE_TIME
    """

    const val SELECT_ACTIVITY_MOVEMENT_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity($LINKED_ACTIVITY_TABLE.search_text, :textSearched)
        END AS similarity_score
    """

    const val GROUP_BY_MOVEMENT = """
         t.$ID, t.$MOVEMENT_DATE_TIME, t.$MOVEMENT_TYPE, t.$MOVEMENT_ACTIVITY_ID, t.$MOVEMENT_REASON, $LINKED_ACTIVITY_TABLE.search_text,
         $LINKED_PROJECT_TABLE.$ID, $LINKED_PROJECT_NAME, $LINKED_PROJECT_START_DATE, $LINKED_PROJECT_START_TIME, $LINKED_PROJECT_END_DATE,
         $LINKED_PROJECT_END_TIME, $LINKED_PROJECT_OPTIONS, $LINKED_ACTIVITY_TABLE.$ID, $MOVEMENT_ACTIVITY_NAME,
         $MOVEMENT_ACTIVITY_DESCRIPTION, $MOVEMENT_ACTIVITY_DURATION, $MOVEMENT_ACTIVITY_MIN_ALLOWED_PARTICIPANTS,
         $MOVEMENT_ACTIVITY_MAX_ALLOWED_PARTICIPANTS, $MOVEMENT_ACTIVITY_START_AVAILABILITY_DATE,
         $MOVEMENT_ACTIVITY_START_AVAILABILITY_TIME, $MOVEMENT_ACTIVITY_END_AVAILABILITY_DATE, $MOVEMENT_ACTIVITY_END_AVAILABILITY_TIME,
         $CREATOR_FIRST_NAME, $CREATOR_LAST_NAME, $CREATOR_EMAIL, $LAST_MODIFIER_FIRST_NAME, $LAST_MODIFIER_LAST_NAME,
         $LAST_MODIFIER_EMAIL, t.$VISIBLE, t.$CREATOR_ID, t.$CREATED_AT, t.$LAST_MODIFIER_ID, t.$LAST_MODIFIER_DATE
    """

    const val ACTIVITY_MOVEMENT_TEXT_SEARCH_CLAUSE =
        "($MOVEMENT_ACTIVITY_ID IS NOT NULL AND (:textSearched IS NULL OR similarity($LINKED_ACTIVITY_TABLE.search_text, :textSearched) > 0))"

    const val ACTIVITY_MOVEMENT_AVAILABILITY_CLAUSE = """
        (
            :availabilitySearched IS NULL OR :availabilitySearched = (
                (
                    COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) < CURRENT_DATE
                    OR (COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                ) AND
                (
                    COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) > CURRENT_DATE
                    OR (COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                )
            )
        )
    """

    const val DATE_IN_ACTIVITY_MOVEMENT_DATES_RANGE_CLAUSE = """
        (
            :dateTimeSearched IS NULL OR (
                (
                    COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) < CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CAST(:dateTimeSearched AS TIME))
                ) AND
                (
                    COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) > CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE($LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CAST(:dateTimeSearched AS TIME))
                )
            )
        )
    """

    const val WITH_CURRENT_MOVEMENT = """
        last_participant_movement AS (
            SELECT t.$ID, t.$MOVEMENT_TYPE, plm.$MOVEMENT_CONTENT_PARTICIPANT_ID
            FROM $MOVEMENT_TABLE t
            INNER JOIN (
                SELECT MAX(t.$MOVEMENT_DATE_TIME) as last_participant_movement_date_time, $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_PARTICIPANT_ID
                FROM $MOVEMENT_TABLE t
                INNER JOIN $MOVEMENT_CONTENT_TABLE ON $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
                WHERE t.$VISIBLE IS TRUE
                GROUP BY $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_PARTICIPANT_ID
            ) AS plm ON plm.last_participant_movement_date_time = t.date_time
        ),
        filtered_groups AS (
            SELECT t.$ID as $GROUP_CONTENT_PARTICIPANT_ID,
            JSON_AGG(
                JSON_BUILD_OBJECT(
                    'id', $GROUP_TABLE.$ID,
                    'name', $GROUP_TABLE.$GROUP_NAME
                )
            ) FILTER (WHERE $GROUP_TABLE.$ID IS NOT NULL) as groups,
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
            ) AS max_end_availability
            FROM $PARTICIPANT_TABLE t
            LEFT JOIN $GROUP_CONTENT_TABLE ON $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID = t.$ID
            LEFT JOIN $GROUP_TABLE ON $GROUP_TABLE.$ID = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID AND $GROUP_TABLE.$VISIBLE IS TRUE
            LEFT JOIN $GROUP_TABLE group_presence ON group_presence.$ID = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID AND group_presence.$VISIBLE IS TRUE
                AND (
                    (
                        COALESCE(group_presence.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) < CURRENT_DATE
                        OR (COALESCE(group_presence.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(group_presence.start_availability_time, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                    ) AND (
                        COALESCE(group_presence.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) > CURRENT_DATE
                        OR (COALESCE(group_presence.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(group_presence.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                    )
                )
            WHERE t.$LINKED_PROJECT_ID = :projectId
            GROUP BY t.$ID
        ),
        current_movement AS (
            SELECT last_participant_movement.$ID, last_participant_movement.$MOVEMENT_CONTENT_PARTICIPANT_ID
            FROM $PARTICIPANT_TABLE t
            LEFT JOIN last_participant_movement ON last_participant_movement.$MOVEMENT_CONTENT_PARTICIPANT_ID = t.$ID
            LEFT JOIN filtered_groups fg ON t.$ID = fg.$GROUP_CONTENT_PARTICIPANT_ID
            WHERE t.$VISIBLE IS TRUE AND t.$PARTICIPANT_PURGED IS FALSE AND t.$LINKED_PROJECT_ID = :projectId AND last_participant_movement.$MOVEMENT_TYPE = (CASE WHEN t.$PARTICIPANT_TYPE = 'REGISTERED' THEN 'OUT' ELSE 'IN' END) AND (
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

    const val CURRENT_MOVEMENT_JOIN = "INNER JOIN current_movement ON current_movement.$ID = t.$ID"
}
