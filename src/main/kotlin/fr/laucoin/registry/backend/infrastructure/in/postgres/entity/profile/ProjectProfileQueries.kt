package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_PURGED
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_PURGED
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LINKED_PROJECT_TABLE

object ProjectProfileQueries {
	const val LINKED_USER_TABLE = "user_tb"
	const val SELECT_LINKED_USER = """
        $LINKED_USER_TABLE.$USER_FIRST_NAME AS $PROJECT_PROFILE_USER_FIRST_NAME,
        $LINKED_USER_TABLE.$USER_LAST_NAME AS $PROJECT_PROFILE_USER_LAST_NAME,
        $LINKED_USER_TABLE.$USER_EMAIL AS $PROJECT_PROFILE_USER_EMAIL,
        $LINKED_USER_TABLE.$USER_LAST_LOGIN AS $PROJECT_PROFILE_USER_LAST_LOGIN,
        $LINKED_USER_TABLE.$USER_PURGED AS $PROJECT_PROFILE_USER_PURGED
    """
	const val JOIN_USER =
		"INNER JOIN $USER_TABLE $LINKED_USER_TABLE ON t.$PROJECT_PROFILE_USER_ID = $LINKED_USER_TABLE.$ID AND $LINKED_USER_TABLE.$VISIBLE IS TRUE"

	const val PROJECT_PROFILE_TEXT_PROJECT_SEARCH_CLAUSE = """
        (
            :textSearched IS NULL OR UNACCENT($LINKED_PROJECT_TABLE.$PROJECT_NAME) ILIKE '%' || UNACCENT(:textSearched) || '%'
        )
    """
	const val SELECT_PROJECT_PROFILE_USER_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity($LINKED_USER_TABLE.search_text, :textSearched)
        END AS similarity_score
    """

	const val PROJECT_PROFILE_TEXT_USER_SEARCH_CLAUSE =
		"(:textSearched IS NULL OR similarity($LINKED_USER_TABLE.search_text, :textSearched) > 0)"

	const val PROJECT_PROFILE_USABLE_CLAUSE = """
        (
            :availabilitySearched IS NULL OR :availabilitySearched = (
                (
                    COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) < CURRENT_DATE
                    OR (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                ) AND
                (
                    COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) > CURRENT_DATE
                    OR (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                )
            )
        )
    """

	const val PROJECT_PROFILE_STATUS_CLAUSE = """
        (t.$PROJECT_PROFILE_STATUS IN (:statusSearched))
    """

	const val DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE = """
        (
            :dateTimeSearched IS NULL OR (
                (
                    COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) < CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) <= CAST(:dateTimeSearched AS TIME))
                ) AND
                (
                    COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) > CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIME) >= CAST(:dateTimeSearched AS TIME))
                )
            )
        )
    """

	const val DATES_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE = """
        (
            (
                :startDateTimeSearched IS NULL OR (
                    COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) > CAST(:startDateTimeSearched AS DATE)
                    OR (COALESCE(t.$PROJECT_PROFILE_END_ACCESS_DATE, '+infinity'::DATE) = CAST(:startDateTimeSearched AS DATE) AND COALESCE(t.$PROJECT_PROFILE_END_ACCESS_TIME, '23:59:59.999999'::TIME) > CAST(:startDateTimeSearched AS TIME))
                )
            ) AND (
                :endDateTimeSearched IS NULL OR (
                    COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) < CAST(:endDateTimeSearched AS DATE)
                    OR (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CAST(:endDateTimeSearched AS DATE) AND COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) <= CAST(:endDateTimeSearched AS TIME))
                )
            )
        )
    """
}
