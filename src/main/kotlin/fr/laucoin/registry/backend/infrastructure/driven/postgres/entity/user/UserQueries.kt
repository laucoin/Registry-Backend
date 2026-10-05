package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_LANGUAGE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_THEME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.PREFERENCE_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_PURGED
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_TYPE

object UserQueries {
	const val SELECT_PREFERENCES = """
        $PREFERENCE_TABLE.$ID AS $PREFERENCE_ID,
        $PREFERENCE_TABLE.$PREFERENCE_THEME,
        $PREFERENCE_TABLE.$PREFERENCE_LANGUAGE
    """

	const val PREFERENCES_JOIN = """
        LEFT JOIN $PREFERENCE_TABLE ON t.$ID = $PREFERENCE_TABLE.$PREFERENCE_USER_ID AND $PREFERENCE_TABLE.$VISIBLE IS TRUE
    """

	const val NOT_PURGED_CLAUSE = "t.$USER_PURGED IS FALSE"

	const val NOT_SERVICE_ACCOUNT = "t.$USER_TYPE <> 'SERVICE_ACCOUNT'"

	const val SELECT_USER_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity(t.search_text, :textSearched)
        END AS similarity_score
    """

	const val USER_TEXT_SEARCH_CLAUSE = "(:textSearched IS NULL OR similarity(t.search_text, :textSearched) > 0)"
}
