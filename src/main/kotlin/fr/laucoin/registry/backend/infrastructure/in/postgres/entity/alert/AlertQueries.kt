package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_STATUS

object AlertQueries {
	const val SELECT_ALERT_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity(t.title, :textSearched)
        END AS similarity_score
    """

	const val ALERT_TEXT_SEARCH_CLAUSE = "(:textSearched IS NULL OR similarity(t.title, :textSearched) > 0)"
	const val ALERT_STATUS_SEARCH_CLAUSE = "(t.$ALERT_STATUS IN (:statusSearched))"

	const val ALERT_DATE_IN_DATES_RANGE_CLAUSE = """
        (
            COALESCE(:startDateTimeSearched, '-infinity'::TIMESTAMP) <= t.$ALERT_DATE_TIME AND
            COALESCE(:endDateTimeSearched, '+infinity'::TIMESTAMP) >= t.$ALERT_DATE_TIME
        )
    """
}
