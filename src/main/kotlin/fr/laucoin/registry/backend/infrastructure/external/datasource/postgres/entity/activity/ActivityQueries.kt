package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME

object ActivityQueries {
    const val SELECT_ACTIVITY_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity(t.search_text, :textSearched)
        END AS similarity_score
    """

    const val ACTIVITY_TEXT_SEARCH_CLAUSE = "(:textSearched IS NULL OR similarity(t.search_text, :textSearched) > 0)"

    const val ACTIVITY_AVAILABILITY_CLAUSE = """
        (
            :availabilitySearched IS NULL OR :availabilitySearched = (
                (
                    COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) < CURRENT_DATE
                    OR (COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                ) AND
                (
                    COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) > CURRENT_DATE
                    OR (COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                )
            )
        )
    """

    const val DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE = """
        (
            :dateTimeSearched IS NULL OR (
                (
                    COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) < CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$ACTIVITY_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$ACTIVITY_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CAST(:dateTimeSearched AS TIME))
                ) AND
                (
                    COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) > CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$ACTIVITY_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$ACTIVITY_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CAST(:dateTimeSearched AS TIME))
                )
            )
        )
    """
}
