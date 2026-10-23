package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_NAME

object ProjectQueries {
	const val PROJECT_TEXT_SEARCH_CLAUSE = """
        (:query IS NULL OR UNACCENT(t.$PROJECT_NAME) ILIKE '%' || UNACCENT(:query) || '%')
    """

	const val DATE_IN_PROJECT_DATES_RANGE_CLAUSE = """
        (
            :at IS NULL OR (
                (
                    (COALESCE(t.$PROJECT_BEGIN_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_BEGIN_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:at AS TIMESTAMPTZ)
                ) AND
                (
                    (COALESCE(t.$PROJECT_END_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_END_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:at AS TIMESTAMPTZ)
                )
            )
        )
    """

	const val PROJECT_SORT = """
        CASE WHEN CAST(:sortField AS TEXT) = 'name' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'name' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'begin' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_BEGIN_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_BEGIN_TIME, '00:00:00.000000'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'begin' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_BEGIN_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_BEGIN_TIME, '00:00:00.000000'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'end' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_END_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_END_TIME, '23:59:59.999999'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'end' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_END_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_END_TIME, '23:59:59.999999'::TIMETZ)) END DESC NULLS LAST,
    """
}
