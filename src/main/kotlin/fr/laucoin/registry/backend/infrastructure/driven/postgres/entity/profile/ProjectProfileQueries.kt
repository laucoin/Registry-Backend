package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_OIDC_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_OIDC_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.LINKED_PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.TEXT_SEARCH_MIN_SCORE

object ProjectProfileQueries {
	const val LINKED_USER_TABLE = "user_tb"
	const val SELECT_LINKED_USER = """
        $LINKED_USER_TABLE.$USER_FIRST_NAME AS $PROJECT_PROFILE_USER_FIRST_NAME,
        $LINKED_USER_TABLE.$USER_LAST_NAME AS $PROJECT_PROFILE_USER_LAST_NAME,
        $LINKED_USER_TABLE.$USER_EMAIL AS $PROJECT_PROFILE_USER_EMAIL,
        $LINKED_USER_TABLE.$USER_LAST_LOGIN AS $PROJECT_PROFILE_USER_LAST_LOGIN,
        $LINKED_USER_TABLE.$USER_OIDC_ID AS $PROJECT_PROFILE_USER_OIDC_ID
    """
	const val JOIN_USER =
		"INNER JOIN $USER_TABLE $LINKED_USER_TABLE ON t.$PROJECT_PROFILE_USER_ID = $LINKED_USER_TABLE.$ID AND $LINKED_USER_TABLE.$VISIBLE IS TRUE"

	const val PROJECT_PROFILE_TEXT_PROJECT_SEARCH_CLAUSE = """
        (
            :query IS NULL OR UNACCENT($LINKED_PROJECT_TABLE.$PROJECT_NAME) ILIKE '%' || UNACCENT(:query) || '%'
        )
    """
	const val SELECT_PROJECT_PROFILE_USER_SEARCH = """
        CASE
            WHEN :query IS NULL THEN 1
            ELSE word_similarity(:query, $LINKED_USER_TABLE.search_text)
        END AS similarity_score
    """

	const val PROJECT_PROFILE_TEXT_USER_SEARCH_CLAUSE =
		"(:query IS NULL OR word_similarity(:query, $LINKED_USER_TABLE.search_text) >= $TEXT_SEARCH_MIN_SCORE)"

	const val PROJECT_PROFILE_USABLE_CLAUSE = """
        (
            :isAvailable IS NULL OR :isAvailable = (
                (
                    (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:#{now} AS TIMESTAMPTZ)
                ) AND
                (
                    (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:#{now} AS TIMESTAMPTZ)
                )
            )
        )
    """

	const val PROJECT_PROFILE_STATUS_CLAUSE = """
        (t.$PROJECT_PROFILE_STATUS IN (:status))
    """

	const val DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE = """
        (
            :at IS NULL OR (
                (
                    (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:at AS TIMESTAMPTZ)
                ) AND
                (
                    (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:at AS TIMESTAMPTZ)
                )
            )
        )
    """

	const val DATES_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE = """
        (
            (
                :from IS NULL OR (
                    COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) > CAST(:from AS DATE)
                    OR (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) = CAST(:from AS DATE) AND COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIME) > CAST(:from AS TIME))
                )
            ) AND (
                :to IS NULL OR (
                    COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) < CAST(:to AS DATE)
                    OR (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CAST(:to AS DATE) AND COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) <= CAST(:to AS TIME))
                )
            )
        )
    """

	const val PROJECT_PROFILE_ROLE_CLAUSE =
		"(CAST(:role AS TEXT) IS NULL OR t.$PROJECT_PROFILE_ROLE = CAST(:role AS TEXT))"

	const val PROJECT_PROFILE_USER_SORT = """
        CASE WHEN CAST(:sortField AS TEXT) = 'firstName' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $LINKED_USER_TABLE.$USER_FIRST_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'firstName' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $LINKED_USER_TABLE.$USER_FIRST_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'lastName' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $LINKED_USER_TABLE.$USER_LAST_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'lastName' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $LINKED_USER_TABLE.$USER_LAST_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'email' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $LINKED_USER_TABLE.$USER_EMAIL END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'email' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $LINKED_USER_TABLE.$USER_EMAIL END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'role' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_PROFILE_ROLE END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'role' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_PROFILE_ROLE END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'status' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_PROFILE_STATUS END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'status' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_PROFILE_STATUS END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'startAccess' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'startAccess' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'endAccess' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'endAccess' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIMETZ)) END DESC NULLS LAST,
    """

	const val PROJECT_PROFILE_PROJECT_SORT = """
        CASE WHEN CAST(:sortField AS TEXT) = 'projectName' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $LINKED_PROJECT_TABLE.$PROJECT_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'projectName' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $LINKED_PROJECT_TABLE.$PROJECT_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'role' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_PROFILE_ROLE END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'role' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_PROFILE_ROLE END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'status' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_PROFILE_STATUS END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'status' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_PROFILE_STATUS END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'startAccess' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'startAccess' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'endAccess' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'endAccess' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIMETZ)) END DESC NULLS LAST,
    """
}
