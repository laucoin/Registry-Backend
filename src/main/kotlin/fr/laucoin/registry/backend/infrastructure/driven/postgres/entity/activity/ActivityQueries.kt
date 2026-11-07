package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.TEXT_SEARCH_MIN_SCORE

object ActivityQueries {
	const val SELECT_ACTIVITY_SEARCH = """
        CASE
            WHEN :#{#c.query} IS NULL THEN 1
            ELSE word_similarity(:#{#c.query}, t.search_text)
        END AS similarity_score
    """

	const val ACTIVITY_TEXT_SEARCH_CLAUSE = "(:#{#c.query} IS NULL OR word_similarity(:#{#c.query}, t.search_text) >= $TEXT_SEARCH_MIN_SCORE)"

	const val ACTIVITY_AVAILABILITY_CLAUSE = """
        (
            :#{#c.isAvailable} IS NULL OR :#{#c.isAvailable} = (
                (
                    (COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:#{now} AS TIMESTAMPTZ)
                ) AND
                (
                    (COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:#{now} AS TIMESTAMPTZ)
                )
            )
        )
    """

	const val DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE = """
        (
            :#{#c.at} IS NULL OR (
                (
                    (COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:#{#c.at} AS TIMESTAMPTZ)
                ) AND
                (
                    (COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:#{#c.at} AS TIMESTAMPTZ)
                )
            )
        )
    """

	const val ACTIVITY_DURATION_CLAUSE = """
        (
            (CAST(:#{#c.minDuration} AS TEXT) IS NULL OR CAST(t.$ACTIVITY_DURATION AS INTERVAL) >= CAST(CAST(:#{#c.minDuration} AS TEXT) AS INTERVAL)) AND
            (CAST(:#{#c.maxDuration} AS TEXT) IS NULL OR CAST(t.$ACTIVITY_DURATION AS INTERVAL) <= CAST(CAST(:#{#c.maxDuration} AS TEXT) AS INTERVAL))
        )
    """

	const val ACTIVITY_PARTICIPANTS_CLAUSE = """
        (
            CAST(:#{#c.participants} AS INTEGER) IS NULL OR (
                (t.$ACTIVITY_MIN_ALLOWED_PARTICIPANTS IS NULL OR t.$ACTIVITY_MIN_ALLOWED_PARTICIPANTS <= CAST(:#{#c.participants} AS INTEGER)) AND
                (t.$ACTIVITY_MAX_ALLOWED_PARTICIPANTS IS NULL OR t.$ACTIVITY_MAX_ALLOWED_PARTICIPANTS >= CAST(:#{#c.participants} AS INTEGER))
            )
        )
    """

	const val ACTIVITY_SORT = """
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'name' AND NOT COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN t.$ACTIVITY_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'name' AND COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN t.$ACTIVITY_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'description' AND NOT COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN t.$ACTIVITY_DESCRIPTION END ASC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'description' AND COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN t.$ACTIVITY_DESCRIPTION END DESC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'duration' AND NOT COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN CAST(t.$ACTIVITY_DURATION AS INTERVAL) END ASC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'duration' AND COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN CAST(t.$ACTIVITY_DURATION AS INTERVAL) END DESC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'minAllowedParticipants' AND NOT COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN t.$ACTIVITY_MIN_ALLOWED_PARTICIPANTS END ASC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'minAllowedParticipants' AND COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN t.$ACTIVITY_MIN_ALLOWED_PARTICIPANTS END DESC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'maxAllowedParticipants' AND NOT COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN t.$ACTIVITY_MAX_ALLOWED_PARTICIPANTS END ASC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'maxAllowedParticipants' AND COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN t.$ACTIVITY_MAX_ALLOWED_PARTICIPANTS END DESC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'startAvailability' AND NOT COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN (COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'startAvailability' AND COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN (COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'endAvailability' AND NOT COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN (COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:#{#page.sortField} AS TEXT) = 'endAvailability' AND COALESCE(CAST(:#{#page.isDescending} AS BOOLEAN), FALSE) THEN (COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) END DESC NULLS LAST,
    """
}
