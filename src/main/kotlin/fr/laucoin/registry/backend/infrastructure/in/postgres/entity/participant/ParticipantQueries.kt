package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.CREATED_AT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_AVAILABLE_GROUPS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_DEPARTED_AT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_TYPE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_TABLE

object ParticipantQueries {
	private const val PARTICIPANT_PREFIX = "participant_"

	/**
	 * One row per participant, and exactly one: `DISTINCT ON` picks their latest
	 * VISIBLE movement, breaking a tie on the recording order. Matching on
	 * `MAX(date_time)` used to emit every movement sharing that instant, so two
	 * records saved for the same minute lent each other their direction and a
	 * returned participant still read as out; callers that size-check the result —
	 * MovementService.validateParticipants compares the row count to the requested
	 * id count — also read the duplicates as "participant not found in project" and
	 * rejected a perfectly valid movement.
	 */
	const val WITH_PARTICIPANT_LAST_MOVEMENT = """
        last_movement AS (
            SELECT DISTINCT ON (mc.$PARTICIPANT_PREFIX$ID)
                mc.$PARTICIPANT_PREFIX$ID,
                t.$MOVEMENT_TYPE,
                t.$MOVEMENT_DATE_TIME AS $PARTICIPANT_LAST_MOVEMENT_DATE_TIME
            FROM $MOVEMENT_TABLE t
            INNER JOIN $MOVEMENT_CONTENT_TABLE mc ON mc.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
            WHERE t.$VISIBLE IS TRUE
            ORDER BY mc.$PARTICIPANT_PREFIX$ID, t.$MOVEMENT_DATE_TIME DESC, t.$CREATED_AT DESC
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
        AND $LINKED_USER_TABLE.$VISIBLE IS TRUE
    """

	const val SELECT_PARTICIPANT_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity(t.search_text, :textSearched)
        END AS similarity_score
    """

	const val PARTICIPANT_TEXT_SEARCH_CLAUSE = "(:textSearched IS NULL OR similarity(t.search_text, :textSearched) > 0)"

	/**
	 * A major was born ON OR BEFORE the date eighteen years ago — earlier date, older
	 * person. The comparison used to be `>=`, which is true for births AFTER that
	 * threshold, i.e. minors: `isMajor = true` then selected minors and the whole
	 * head-count came out swapped. Only visible when the two populations differ in
	 * size, which is why it survived: with one major and one minor the swap returns
	 * the same pair of counts. Equality is deliberate — someone turning eighteen
	 * today is a major.
	 */
	const val PARTICIPANT_MAJOR_CLAUSE =
		"(:isMajor IS NULL OR :isMajor = (t.$PARTICIPANT_BIRTHDAY <= CURRENT_DATE - INTERVAL '18 years'))"

	const val PARTICIPANT_TYPE_SEARCH_CLAUSE = "(:typeSearched IS NULL OR t.type = :typeSearched)"

	/**
	 * A birthday recurs: it is the DAY and MONTH that fall today, never the year
	 * the person was born. Comparing the stored date to CURRENT_DATE matched only
	 * someone born today, so the card was empty every day of the project.
	 * `TO_CHAR` keeps 29 February honest — it simply finds nobody in a non-leap
	 * year, which is what the calendar says.
	 */
	const val PARTICIPANT_BIRTHDAY_TODAY_CLAUSE =
		"TO_CHAR(t.$PARTICIPANT_BIRTHDAY, 'MM-DD') = TO_CHAR(CURRENT_DATE, 'MM-DD')"

	/**
	 * "The participant is expected on site right now": their own window contains
	 * now, and — when they carry no dates of their own — at least one of their
	 * groups is open, since that is the window they actually run on.
	 *
	 * The group term cannot be left to the COALESCE below. `filtered_groups` joins
	 * only the groups that are open now, so a group that has not opened yet leaves
	 * NULL columns behind and the CASE inside the CTE turns those into
	 * -infinity/+infinity: group bounds could never make anyone unavailable, and a
	 * member of a group opening next month was listed as available while the very
	 * status returned beside them — computed from the same rule as
	 * [DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE], in `buildStatus` — read
	 * UNAVAILABLE.
	 */
	const val PARTICIPANT_AVAILABLE_EXPRESSION = """
        (
            (
                fg.$PARTICIPANT_GROUPS IS NULL OR json_array_length(fg.$PARTICIPANT_GROUPS) = 0
                OR (fg.$PARTICIPANT_AVAILABLE_GROUPS IS NOT NULL AND json_array_length(fg.$PARTICIPANT_AVAILABLE_GROUPS) > 0)
            ) AND
            (
                COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, CAST(fg.min_start_availability AS DATE), '+infinity'::DATE) < CURRENT_DATE
                OR (COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, CAST(fg.min_start_availability AS DATE), '+infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$PARTICIPANT_START_AVAILABILITY_TIME, CAST(fg.min_start_availability AS TIME), '00:00:00.000000'::TIME) <= CURRENT_TIME)
            ) AND
            (
                COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, CAST(fg.max_end_availability AS DATE), '-infinity'::DATE) > CURRENT_DATE
                OR (COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, CAST(fg.max_end_availability AS DATE), '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$PARTICIPANT_END_AVAILABILITY_TIME, CAST(fg.max_end_availability AS TIME), '23:59:59.999999'::TIME) >= CURRENT_TIME)
            )
        )
    """

	const val PARTICIPANT_AVAILABILITY_CLAUSE =
		"(:availabilitySearched IS NULL OR :availabilitySearched = $PARTICIPANT_AVAILABLE_EXPRESSION)"

	/**
	 * The SQL twin of `AvailabilityElementExt.status`, and it must stay one:
	 * ParticipantStatusParityTest walks a fixture matrix through both. Departure is
	 * answered first and from the register's own column, then the window is allowed
	 * to speak only for someone no movement describes — everyone who has moved
	 * lands in IN or OUT, so no head count can drop them.
	 */
	const val PARTICIPANT_STATUS_EXPRESSION = """
        (
            CASE
                WHEN t.$PARTICIPANT_DEPARTED_AT IS NOT NULL THEN 'DEPARTED'
                WHEN last_movement.type = 'IN' THEN 'IN'
                WHEN last_movement.type IS NULL AND NOT $PARTICIPANT_AVAILABLE_EXPRESSION THEN 'UNAVAILABLE'
                ELSE 'OUT'
            END
        )
    """

	const val PARTICIPANT_STATUS_CLAUSE =
		"(:statusSearched IS NULL OR :statusSearched = $PARTICIPANT_STATUS_EXPRESSION)"

	const val PARTICIPANT_WARNING_EXPRESSION = """
        (
            t.$PARTICIPANT_DEPARTED_AT IS NULL
            AND last_movement.type IS NOT NULL
            AND NOT $PARTICIPANT_AVAILABLE_EXPRESSION
        )
    """

	const val PARTICIPANT_WARNED_CLAUSE =
		"(:warnedSearched IS NULL OR :warnedSearched = $PARTICIPANT_WARNING_EXPRESSION)"

	const val PARTICIPANT_DEPARTED_CLAUSE =
		"(:departedSearched IS NULL OR :departedSearched = (t.$PARTICIPANT_DEPARTED_AT IS NOT NULL))"

	/**
	 * The "no group" bucket the presence board lists apart: `false` keeps only the
	 * participants no VISIBLE group holds. `filtered_groups` aggregates under a
	 * FILTER, so the column is NULL — never an empty array — when a participant has
	 * none, and `json_array_length` is strict, so the test reads the same way in
	 * both directions.
	 */
	const val PARTICIPANT_GROUPED_CLAUSE = """
        (
            :groupedSearched IS NULL OR :groupedSearched = (
                fg.$PARTICIPANT_GROUPS IS NOT NULL AND json_array_length(fg.$PARTICIPANT_GROUPS) > 0
            )
        )
    """

	/**
	 * "scheduled to arrive today": the participant's EFFECTIVE availability
	 * window starts today, where effective = the participant's own start if set,
	 * otherwise the earliest of their groups' starts (participant window takes
	 * precedence). Computed in a CTE so the outer SELECT stays one row/participant.
	 */
	const val WITH_ARRIVING_TODAY = """
        arriving_today AS (
            SELECT t.$ID AS $GROUP_CONTENT_PARTICIPANT_ID
            FROM $PARTICIPANT_TABLE t
            LEFT JOIN $GROUP_CONTENT_TABLE gc ON gc.$GROUP_CONTENT_PARTICIPANT_ID = t.$ID
            LEFT JOIN $GROUP_TABLE g ON g.$ID = gc.$GROUP_CONTENT_GROUP_ID AND g.$VISIBLE IS TRUE
            WHERE t.$LINKED_PROJECT_ID = :projectId
            GROUP BY t.$ID, t.$PARTICIPANT_START_AVAILABILITY_DATE
            HAVING COALESCE(t.$PARTICIPANT_START_AVAILABILITY_DATE, MIN(g.$GROUP_START_AVAILABILITY_DATE)) = CURRENT_DATE
        )
    """

	const val ARRIVING_TODAY_JOIN =
		"INNER JOIN arriving_today ON arriving_today.$GROUP_CONTENT_PARTICIPANT_ID = t.$ID"

	/**
	 * A scheduled arrival hasn't checked in yet: no current movement, or the last
	 * one is an exit. Registered participants only (guests aren't scheduled).
	 */
	const val ARRIVING_TODAY_NOT_PRESENT_CLAUSE =
		"((last_movement.type IS NULL OR last_movement.type = 'OUT') AND t.$PARTICIPANT_TYPE = 'REGISTERED' AND t.$PARTICIPANT_DEPARTED_AT IS NULL)"

	/**
	 * Departures today — the mirror of arrivals: effective availability window
	 * (participant's own end date, else the group's latest) ENDS today.
	 */
	const val WITH_DEPARTING_TODAY = """
        departing_today AS (
            SELECT t.$ID AS $GROUP_CONTENT_PARTICIPANT_ID
            FROM $PARTICIPANT_TABLE t
            LEFT JOIN $GROUP_CONTENT_TABLE gc ON gc.$GROUP_CONTENT_PARTICIPANT_ID = t.$ID
            LEFT JOIN $GROUP_TABLE g ON g.$ID = gc.$GROUP_CONTENT_GROUP_ID AND g.$VISIBLE IS TRUE
            WHERE t.$LINKED_PROJECT_ID = :projectId
            GROUP BY t.$ID, t.$PARTICIPANT_END_AVAILABILITY_DATE
            HAVING COALESCE(t.$PARTICIPANT_END_AVAILABILITY_DATE, MAX(g.$GROUP_END_AVAILABILITY_DATE)) = CURRENT_DATE
        )
    """

	const val DEPARTING_TODAY_JOIN =
		"INNER JOIN departing_today ON departing_today.$GROUP_CONTENT_PARTICIPANT_ID = t.$ID"

	/**
	 * A scheduled departure is still on site: their current movement is an entry.
	 * Registered participants only (guests aren't scheduled).
	 */
	const val DEPARTING_TODAY_PRESENT_CLAUSE =
		"(last_movement.type = 'IN' AND t.$PARTICIPANT_TYPE = 'REGISTERED' AND t.$PARTICIPANT_DEPARTED_AT IS NULL)"

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
