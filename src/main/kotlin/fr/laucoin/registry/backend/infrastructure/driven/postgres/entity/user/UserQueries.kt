package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_LANGUAGE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_THEME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.PREFERENCE_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_TYPE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.TEXT_SEARCH_MIN_SCORE

object UserQueries {
	const val SELECT_PREFERENCES = """
        $PREFERENCE_TABLE.$ID AS $PREFERENCE_ID,
        $PREFERENCE_TABLE.$PREFERENCE_THEME,
        $PREFERENCE_TABLE.$PREFERENCE_LANGUAGE
    """

	const val PREFERENCES_JOIN = """
        LEFT JOIN $PREFERENCE_TABLE ON t.$ID = $PREFERENCE_TABLE.$PREFERENCE_USER_ID AND $PREFERENCE_TABLE.$VISIBLE IS TRUE
    """


	const val NOT_SERVICE_ACCOUNT = "t.$USER_TYPE <> 'SERVICE_ACCOUNT'"

	const val SELECT_USER_SEARCH = """
        CASE
            WHEN :query IS NULL THEN 1
            ELSE word_similarity(:query, t.search_text)
        END AS similarity_score
    """

	const val USER_TEXT_SEARCH_CLAUSE = "(:query IS NULL OR word_similarity(:query, t.search_text) >= $TEXT_SEARCH_MIN_SCORE)"

	const val USER_ROLE_CLAUSE = "(CAST(:role AS TEXT) IS NULL OR t.$USER_ROLE = CAST(:role AS TEXT))"

	const val USER_SORT = """
        CASE WHEN CAST(:sortField AS TEXT) = 'firstName' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_FIRST_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'firstName' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_FIRST_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'lastName' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_LAST_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'lastName' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_LAST_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'email' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_EMAIL END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'email' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_EMAIL END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'role' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_ROLE END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'role' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_ROLE END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'birthday' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_BIRTHDAY END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'birthday' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_BIRTHDAY END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'lastLogin' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_LAST_LOGIN END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'lastLogin' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$USER_LAST_LOGIN END DESC NULLS LAST,
    """
}
