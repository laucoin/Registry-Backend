package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_NAME

object ProjectQueries {
	const val PROJECT_TEXT_SEARCH_CLAUSE = """
        (:textSearched IS NULL OR UNACCENT(t.$PROJECT_NAME) ILIKE '%' || UNACCENT(:textSearched) || '%')
    """

	const val DATE_IN_PROJECT_DATES_RANGE_CLAUSE = """
        (
            :dateTimeSearched IS NULL OR (
                (
                    COALESCE(t.$PROJECT_BEGIN_DATE, '-infinity'::DATE) < CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$PROJECT_BEGIN_DATE, '-infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$PROJECT_BEGIN_TIME, '00:00:00.000000'::TIME) <= CAST(:dateTimeSearched AS TIME))
                ) AND
                (
                    COALESCE(t.$PROJECT_END_DATE, '+infinity'::DATE) > CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$PROJECT_END_DATE, '+infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$PROJECT_END_TIME, '23:59:59.999999'::TIME) >= CAST(:dateTimeSearched AS TIME))
                )
            )
        )
    """
}
