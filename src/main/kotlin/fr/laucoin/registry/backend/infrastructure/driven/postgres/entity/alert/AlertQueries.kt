package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_CRITICALITY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_LAST_COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_LAST_COMMUNICATION_MESSAGE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_STATUS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_TITLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_ALERT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_MESSAGE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.TEXT_SEARCH_MIN_SCORE

object AlertQueries {
	const val SELECT_ALERT_SEARCH = """
        CASE
            WHEN :query IS NULL THEN 1
            ELSE word_similarity(:query, t.search_text)
        END AS similarity_score
    """

	const val ALERT_CRITICALITY_EXPRESSION = """
        CASE
            WHEN t.$ALERT_STATUS <> 'IN_PROGRESS' THEN NULL
            WHEN t.$ALERT_DATE_TIME <= CAST(:#{now} AS TIMESTAMPTZ) - (:highAfterHours * INTERVAL '1 hour') THEN 'HIGH'
            WHEN t.$ALERT_DATE_TIME <= CAST(:#{now} AS TIMESTAMPTZ) - (:reinforcedAfterHours * INTERVAL '1 hour') THEN 'REINFORCED'
            ELSE 'RECENT'
        END
    """

	const val ALERT_CRITICALITY_RANK = """
        CASE $ALERT_CRITICALITY_EXPRESSION WHEN 'HIGH' THEN 3 WHEN 'REINFORCED' THEN 2 WHEN 'RECENT' THEN 1 END
    """

	const val ALERT_LAST_COMMUNICATION_JOIN = """
        LEFT JOIN LATERAL (
            SELECT tc.$COMMUNICATION_DATE_TIME AS $ALERT_LAST_COMMUNICATION_DATE_TIME, tc.$COMMUNICATION_MESSAGE AS $ALERT_LAST_COMMUNICATION_MESSAGE
            FROM $COMMUNICATION_TABLE tc
            WHERE tc.$COMMUNICATION_ALERT_ID = t.$ID AND tc.$VISIBLE
            ORDER BY tc.$COMMUNICATION_DATE_TIME DESC, tc.$ID DESC
            LIMIT 1
        ) lc ON TRUE
    """

	const val SELECT_ALERT_LAST_COMMUNICATION = "lc.$ALERT_LAST_COMMUNICATION_DATE_TIME, lc.$ALERT_LAST_COMMUNICATION_MESSAGE"

	const val SELECT_ALERT_CRITICALITY = "$ALERT_CRITICALITY_EXPRESSION AS $ALERT_CRITICALITY"

	const val ALERT_CRITICALITY_CLAUSE = "(:criticality IS NULL OR $ALERT_CRITICALITY_EXPRESSION = :criticality)"

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
        CASE WHEN CAST(:sortField AS TEXT) = 'criticality' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $ALERT_CRITICALITY_RANK END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'criticality' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $ALERT_CRITICALITY_RANK END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'criticality' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$ALERT_DATE_TIME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'criticality' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$ALERT_DATE_TIME END ASC NULLS LAST,
    """
}
