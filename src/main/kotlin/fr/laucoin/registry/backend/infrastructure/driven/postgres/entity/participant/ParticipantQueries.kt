package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_REASON
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_EFFECTIVE_ARRIVAL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_AVAILABLE_GROUPS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_EFFECTIVE_DEPARTURE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_STATUS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_TYPE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.TEXT_SEARCH_MIN_SCORE

object ParticipantQueries {
	private const val PARTICIPANT_PREFIX = "participant_"
	private const val LAST_MOVEMENT_HEAD = """
        last_movement AS (
            SELECT plm.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME, plm.$PARTICIPANT_PREFIX$ID, t.$MOVEMENT_TYPE
            FROM $MOVEMENT_TABLE t
            INNER JOIN (
                SELECT MAX(t.$MOVEMENT_DATE_TIME) as $PARTICIPANT_LAST_MOVEMENT_DATE_TIME, $MOVEMENT_CONTENT_TABLE.$PARTICIPANT_PREFIX$ID
                FROM $MOVEMENT_TABLE t
                INNER JOIN $MOVEMENT_CONTENT_TABLE ON $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
                WHERE
    """

	private const val LAST_MOVEMENT_INNER_PROJECT_FILTER = "t.$LINKED_PROJECT_ID = :projectId AND"

	private const val LAST_MOVEMENT_BODY = """
                t.$VISIBLE IS TRUE
                GROUP BY $MOVEMENT_CONTENT_TABLE.$PARTICIPANT_PREFIX$ID
            ) AS plm ON plm.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME = t.$MOVEMENT_DATE_TIME
            INNER JOIN $MOVEMENT_CONTENT_TABLE last_content
                ON last_content.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID AND last_content.$PARTICIPANT_PREFIX$ID = plm.$PARTICIPANT_PREFIX$ID
    """

	private const val LAST_MOVEMENT_OUTER_PROJECT_FILTER = "WHERE t.$LINKED_PROJECT_ID = :projectId"

	private const val LAST_MOVEMENT_TAIL = ")"

	const val WITH_PARTICIPANT_LAST_MOVEMENT =
		"$LAST_MOVEMENT_HEAD $LAST_MOVEMENT_INNER_PROJECT_FILTER $LAST_MOVEMENT_BODY $LAST_MOVEMENT_OUTER_PROJECT_FILTER $LAST_MOVEMENT_TAIL"

	const val WITH_PARTICIPANT_LAST_MOVEMENT_ALL_PROJECTS = "$LAST_MOVEMENT_HEAD $LAST_MOVEMENT_BODY $LAST_MOVEMENT_TAIL"

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
            BOOL_OR($GROUP_TABLE.$GROUP_END_AVAILABILITY_DATE IS NULL) FILTER (WHERE $GROUP_TABLE.$ID IS NOT NULL) AS has_open_ended_group,
            JSON_AGG(group_presence.$ID) FILTER (WHERE group_presence.$ID IS NOT NULL) as $PARTICIPANT_AVAILABLE_GROUPS,
            JSON_AGG(group_presence_on_date.$ID) FILTER (WHERE group_presence_on_date.$ID IS NOT NULL) as available_groups_on_date
            FROM $PARTICIPANT_TABLE t
            LEFT JOIN $GROUP_CONTENT_TABLE ON $GROUP_CONTENT_TABLE.$PARTICIPANT_PREFIX$ID = t.$ID
            LEFT JOIN $GROUP_TABLE ON $GROUP_TABLE.$ID = $GROUP_CONTENT_TABLE.$GROUP_PREFIX$ID AND $GROUP_TABLE.$VISIBLE IS TRUE
            LEFT JOIN $GROUP_TABLE group_presence ON group_presence.$ID = $GROUP_CONTENT_TABLE.$GROUP_PREFIX$ID AND group_presence.$VISIBLE IS TRUE
                AND (
                    (
                        (COALESCE(group_presence.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(group_presence.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:#{now} AS TIMESTAMPTZ)
                    ) AND (
                        (COALESCE(group_presence.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(group_presence.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:#{now} AS TIMESTAMPTZ)
                    )
                )
            LEFT JOIN $GROUP_TABLE group_presence_on_date ON group_presence_on_date.$ID = $GROUP_CONTENT_TABLE.$GROUP_PREFIX$ID AND group_presence_on_date.$VISIBLE IS TRUE
                AND :at IS NULL OR (
                    (
                        (COALESCE(group_presence_on_date.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(group_presence_on_date.$GROUP_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:at AS TIMESTAMPTZ)
                    ) AND (
                        (COALESCE(group_presence_on_date.$GROUP_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(group_presence_on_date.$GROUP_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:at AS TIMESTAMPTZ)
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
        AND $LINKED_USER_TABLE.$VISIBLE IS TRUE
    """


	const val SELECT_PARTICIPANT_SEARCH = """
        CASE
            WHEN :query IS NULL THEN 1
            ELSE word_similarity(:query, t.search_text)
        END AS similarity_score
    """

	const val PARTICIPANT_TEXT_SEARCH_CLAUSE = "(:query IS NULL OR word_similarity(:query, t.search_text) >= $TEXT_SEARCH_MIN_SCORE)"

	// Un participant est majeur lorsqu'il est né au plus tard aujourd'hui moins 18 ans.
	// Reflète DateExt.isMajor(), sur lequel s'appuie la vérification du conducteur — garder les deux synchronisés.
	const val PARTICIPANT_MAJOR_CLAUSE =
		"(:isMajor IS NULL OR :isMajor = (t.$PARTICIPANT_BIRTHDAY <= CAST(:#{now} AS DATE) - INTERVAL '18 years'))"

	const val PARTICIPANT_TYPE_SEARCH_CLAUSE = "(:type IS NULL OR t.type = :type)"

	const val UPDATE_PARTICIPANT_EFFECTIVE_DATES = """
        UPDATE $PARTICIPANT_TABLE p SET
            $PARTICIPANT_EFFECTIVE_ARRIVAL = (
                SELECT MIN(m.$MOVEMENT_DATE_TIME)
                FROM $MOVEMENT_TABLE m
                INNER JOIN $MOVEMENT_CONTENT_TABLE mc ON mc.$MOVEMENT_CONTENT_MOVEMENT_ID = m.$ID
                WHERE mc.$MOVEMENT_CONTENT_PARTICIPANT_ID = p.$ID AND m.$VISIBLE IS TRUE AND m.$MOVEMENT_TYPE = 'IN'
            ),
            $PARTICIPANT_EFFECTIVE_DEPARTURE = (
                SELECT MAX(m.$MOVEMENT_DATE_TIME)
                FROM $MOVEMENT_TABLE m
                INNER JOIN $MOVEMENT_CONTENT_TABLE mc ON mc.$MOVEMENT_CONTENT_MOVEMENT_ID = m.$ID
                WHERE mc.$MOVEMENT_CONTENT_PARTICIPANT_ID = p.$ID AND m.$VISIBLE IS TRUE AND m.$MOVEMENT_TYPE = 'OUT'
                    AND (m.$MOVEMENT_REASON = 'DEFINITIVE_DEPARTURE' OR p.$PARTICIPANT_TYPE = 'GUEST')
                    AND NOT EXISTS (
                        SELECT 1
                        FROM $MOVEMENT_TABLE later
                        INNER JOIN $MOVEMENT_CONTENT_TABLE later_content ON later_content.$MOVEMENT_CONTENT_MOVEMENT_ID = later.$ID
                        WHERE later_content.$MOVEMENT_CONTENT_PARTICIPANT_ID = p.$ID AND later.$VISIBLE IS TRUE
                            AND later.$MOVEMENT_TYPE = 'IN' AND later.$MOVEMENT_DATE_TIME > m.$MOVEMENT_DATE_TIME
                    )
            )
        WHERE p.$ID IN (:ids)
    """

	const val PARTICIPANT_NOT_DEPARTED_CLAUSE = "t.$PARTICIPANT_EFFECTIVE_DEPARTURE IS NULL"

	const val PARTICIPANT_IN_PLANNED_RANGE = """
        (
            (
                COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, CAST(fg.min_start_availability AS DATE), '+infinity'::DATE) < CAST(:#{now} AS DATE)
                OR (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, CAST(fg.min_start_availability AS DATE), '+infinity'::DATE) = CAST(:#{now} AS DATE) AND COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, CAST(fg.min_start_availability AS TIME), '00:00:00.000000'::TIME) <= CAST(:#{now} AS TIMETZ))
            ) AND
            (
                COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, CAST(fg.max_end_availability AS DATE), '-infinity'::DATE) > CAST(:#{now} AS DATE)
                OR (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, CAST(fg.max_end_availability AS DATE), '-infinity'::DATE) = CAST(:#{now} AS DATE) AND COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, CAST(fg.max_end_availability AS TIME), '23:59:59.999999'::TIME) >= CAST(:#{now} AS TIMETZ))
            )
        )
    """

	const val PARTICIPANT_PRESENCE_EXPRESSION = """
        CASE
            WHEN t.$PARTICIPANT_EFFECTIVE_DEPARTURE IS NOT NULL THEN 'UNAVAILABLE'
            WHEN t.$PARTICIPANT_EFFECTIVE_ARRIVAL IS NOT NULL THEN (CASE WHEN last_movement.$MOVEMENT_TYPE = 'IN' THEN 'IN' ELSE 'OUT' END)
            WHEN $PARTICIPANT_IN_PLANNED_RANGE THEN 'OUT'
            ELSE 'UNAVAILABLE'
        END
    """

	const val PARTICIPANT_PLANNED_END_PASSED = """
        (
            (
                t.$PARTICIPANT_END_AVAILABILITY_DATE IS NOT NULL
                AND (t.$PARTICIPANT_END_AVAILABILITY_DATE + COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) < CAST(:#{now} AS TIMESTAMPTZ)
            ) OR (
                t.$PARTICIPANT_END_AVAILABILITY_DATE IS NULL
                AND fg.$PARTICIPANT_GROUPS IS NOT NULL
                AND fg.has_open_ended_group IS FALSE
                AND fg.$PARTICIPANT_AVAILABLE_GROUPS IS NULL
            )
        )
    """

	const val PARTICIPANT_STATUS_EXPRESSION = """
        CASE
            WHEN t.$PARTICIPANT_EFFECTIVE_ARRIVAL IS NOT NULL AND t.$PARTICIPANT_EFFECTIVE_DEPARTURE IS NULL
                AND $PARTICIPANT_PLANNED_END_PASSED THEN 'OVERDUE'
            ELSE $PARTICIPANT_PRESENCE_EXPRESSION
        END
    """

	const val PARTICIPANT_STATUS_RANK = """
        CASE $PARTICIPANT_STATUS_EXPRESSION
            WHEN 'OVERDUE' THEN 0
            WHEN 'OUT' THEN 1
            WHEN 'IN' THEN 2
            ELSE 3
        END
    """

	const val SELECT_LAST_MOVEMENT = """
        $PARTICIPANT_STATUS_EXPRESSION AS $PARTICIPANT_STATUS,
        last_movement.$PARTICIPANT_LAST_MOVEMENT_DATE_TIME
    """

	const val PARTICIPANT_AVAILABILITY_CLAUSE =
		"(:isAvailable IS NULL OR :isAvailable = ($PARTICIPANT_IN_PLANNED_RANGE AND $PARTICIPANT_NOT_DEPARTED_CLAUSE))"

	const val PARTICIPANT_STATUS_CLAUSE = "(:status IS NULL OR $PARTICIPANT_STATUS_EXPRESSION = :status)"

	const val PARTICIPANT_PRESENCE_CLAUSE = "(:presence IS NULL OR $PARTICIPANT_PRESENCE_EXPRESSION = :presence)"

	const val DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE = """
        (
            :at IS NULL OR (
                (
                    fg.$PARTICIPANT_GROUPS IS NULL OR json_array_length(fg.$PARTICIPANT_GROUPS) = 0
                    OR (fg.available_groups_on_date IS NOT NULL AND json_array_length(fg.available_groups_on_date) > 0)
                ) AND
                (
                    (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:at AS TIMESTAMPTZ)
                ) AND
                (
                    (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:at AS TIMESTAMPTZ)
                ) AND
                (t.$PARTICIPANT_EFFECTIVE_DEPARTURE IS NULL OR t.$PARTICIPANT_EFFECTIVE_DEPARTURE > CAST(:at AS TIMESTAMPTZ))
            )
        )
    """

	const val PARTICIPANT_SORT = """
        CASE WHEN CAST(:sortField AS TEXT) = 'firstName' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PARTICIPANT_FIRST_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'firstName' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PARTICIPANT_FIRST_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'lastName' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PARTICIPANT_LAST_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'lastName' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PARTICIPANT_LAST_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'birthday' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PARTICIPANT_BIRTHDAY END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'birthday' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PARTICIPANT_BIRTHDAY END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'type' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PARTICIPANT_TYPE END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'type' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PARTICIPANT_TYPE END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'startAvailability' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'startAvailability' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'endAvailability' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'endAvailability' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'status' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $PARTICIPANT_STATUS_RANK END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'status' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $PARTICIPANT_STATUS_RANK END DESC NULLS LAST,
    """

	const val PARTICIPANT_ON_SITE_CLAUSE = """
        (
            CAST(:isOnSite AS BOOLEAN) IS NULL
            OR CAST(:isOnSite AS BOOLEAN) = (t.$PARTICIPANT_EFFECTIVE_ARRIVAL IS NOT NULL AND t.$PARTICIPANT_EFFECTIVE_DEPARTURE IS NULL)
        )
    """

	const val PARTICIPANT_USER_LINK_CLAUSE =
		"(CAST(:hasUser AS BOOLEAN) IS NULL OR CAST(:hasUser AS BOOLEAN) = (t.$PARTICIPANT_USER_ID IS NOT NULL))"

	const val PARTICIPANT_BIRTHDAY_CLAUSE = """
        (
            (CAST(:birthdayFrom AS DATE) IS NULL OR t.$PARTICIPANT_BIRTHDAY >= CAST(:birthdayFrom AS DATE)) AND
            (CAST(:birthdayTo AS DATE) IS NULL OR t.$PARTICIPANT_BIRTHDAY <= CAST(:birthdayTo AS DATE))
        )
    """

	const val PARTICIPANT_PLANNED_ELIGIBLE_CLAUSE = """
        (
            t.$PARTICIPANT_TYPE = 'REGISTERED'
            AND t.$PARTICIPANT_EFFECTIVE_DEPARTURE IS NULL
            AND (NOT :isArrival OR last_movement.$MOVEMENT_TYPE IS DISTINCT FROM 'IN')
        )
    """

	const val PARTICIPANT_OWN_PLANNED_DATE_CLAUSE = """
        (CASE WHEN :isArrival THEN t.$PARTICIPANT_START_AVAILABILITY_DATE ELSE t.$PARTICIPANT_END_AVAILABILITY_DATE END) = :day
    """

	const val PARTICIPANT_WITHOUT_OWN_PLANNED_DATE_CLAUSE = """
        (CASE WHEN :isArrival THEN t.$PARTICIPANT_START_AVAILABILITY_DATE ELSE t.$PARTICIPANT_END_AVAILABILITY_DATE END) IS NULL
    """

	const val GROUP_PLANNED_DATE_CLAUSE = """
        (CASE WHEN :isArrival THEN g.$GROUP_START_AVAILABILITY_DATE ELSE g.$GROUP_END_AVAILABILITY_DATE END) = :day
    """

	const val GROUP_DRIVES_PARTICIPANT_PLANNED_DATE_CLAUSE = """
        NOT EXISTS (
            SELECT 1
            FROM $GROUP_CONTENT_TABLE other_content
            INNER JOIN $GROUP_TABLE other_group ON other_group.$ID = other_content.$GROUP_CONTENT_GROUP_ID AND other_group.$VISIBLE IS TRUE
            WHERE other_content.$GROUP_CONTENT_PARTICIPANT_ID = t.$ID AND (
                CASE WHEN :isArrival
                    THEN COALESCE(other_group.$GROUP_START_AVAILABILITY_DATE, '-infinity'::DATE) < g.$GROUP_START_AVAILABILITY_DATE
                    ELSE (other_group.$GROUP_END_AVAILABILITY_DATE IS NULL OR other_group.$GROUP_END_AVAILABILITY_DATE > g.$GROUP_END_AVAILABILITY_DATE)
                END
            )
        )
    """
}
