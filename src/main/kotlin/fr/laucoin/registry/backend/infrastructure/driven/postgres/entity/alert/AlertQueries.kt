package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_STATUS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_TITLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.TEXT_SEARCH_MIN_SCORE

object AlertQueries {
	const val SELECT_ALERT_SEARCH = """
        CASE
            WHEN :query IS NULL THEN 1
            ELSE word_similarity(:query, t.search_text)
        END AS similarity_score
    """

	const val ALERT_TEXT_SEARCH_CLAUSE = "(:query IS NULL OR word_similarity(:query, t.search_text) >= $TEXT_SEARCH_MIN_SCORE)"
	const val ALERT_STATUS_SEARCH_CLAUSE = "(t.$ALERT_STATUS IN (:status))"

	const val ALERT_DATE_IN_DATES_RANGE_CLAUSE = """
        (
            COALESCE(:from, '-infinity'::TIMESTAMP) <= t.$ALERT_DATE_TIME AND
            COALESCE(:to, '+infinity'::TIMESTAMP) >= t.$ALERT_DATE_TIME
        )
    """

	const val ALERT_SORT = """
        CASE WHEN CAST(:sortField AS TEXT) = 'title' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$ALERT_TITLE END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'title' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$ALERT_TITLE END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'dateTime' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$ALERT_DATE_TIME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'dateTime' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$ALERT_DATE_TIME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'status' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$ALERT_STATUS END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'status' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$ALERT_STATUS END DESC NULLS LAST,
    """
}
