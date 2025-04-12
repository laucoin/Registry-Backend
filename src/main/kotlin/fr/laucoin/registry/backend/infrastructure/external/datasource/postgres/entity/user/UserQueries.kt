package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_START_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_START_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.PREFERENCE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TYPE

object UserQueries {
    const val SELECT_PREFERENCES = """
        $PREFERENCE_TABLE.$ID AS $PREFERENCE_ID,
        $PREFERENCE_TABLE.$PREFERENCE_SELECTED_PROFILE_ID AS $PREFERENCE_SELECTED_PROFILE_ID,
        $EVENT_TABLE.$ID AS $PREFERENCE_SELECTED_PROFILE_EVENT_ID,
        $EVENT_TABLE.$EVENT_NAME AS $PREFERENCE_SELECTED_PROFILE_EVENT_NAME,
        $EVENT_TABLE.$EVENT_BEGIN_DATE AS $PREFERENCE_SELECTED_PROFILE_EVENT_START_DATE,
        $EVENT_TABLE.$EVENT_BEGIN_TIME AS $PREFERENCE_SELECTED_PROFILE_EVENT_START_TIME,
        $EVENT_TABLE.$EVENT_END_DATE AS $PREFERENCE_SELECTED_PROFILE_EVENT_END_DATE,
        $EVENT_TABLE.$EVENT_END_TIME AS $PREFERENCE_SELECTED_PROFILE_EVENT_END_TIME,
        $EVENT_TABLE.$EVENT_OPTIONS AS $PREFERENCE_SELECTED_PROFILE_EVENT_OPTIONS,
        $EVENT_PROFILE_TABLE.$EVENT_PROFILE_ROLE AS $PREFERENCE_SELECTED_PROFILE_ROLE,
        $EVENT_PROFILE_TABLE.$EVENT_PROFILE_STATUS AS $PREFERENCE_SELECTED_PROFILE_STATUS,
        $EVENT_PROFILE_TABLE.$EVENT_PROFILE_START_ACCESS_DATE AS $PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE,
        $EVENT_PROFILE_TABLE.$EVENT_PROFILE_START_ACCESS_TIME AS $PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME,
        $EVENT_PROFILE_TABLE.$EVENT_PROFILE_END_ACCESS_DATE AS $PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE,
        $EVENT_PROFILE_TABLE.$EVENT_PROFILE_END_ACCESS_TIME AS $PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME
    """

    const val PREFERENCES_JOIN = """
        LEFT JOIN $PREFERENCE_TABLE ON t.$ID = $PREFERENCE_TABLE.$PREFERENCE_USER_ID AND $PREFERENCE_TABLE.$VISIBLE IS TRUE
        LEFT JOIN $EVENT_PROFILE_TABLE ON $PREFERENCE_TABLE.$PREFERENCE_SELECTED_PROFILE_ID = $EVENT_PROFILE_TABLE.$ID AND $EVENT_PROFILE_TABLE.$VISIBLE IS TRUE
        LEFT JOIN $EVENT_TABLE ON $EVENT_PROFILE_TABLE.$LINKED_EVENT_ID = $EVENT_TABLE.$ID AND $EVENT_TABLE.$VISIBLE IS TRUE
    """

    const val NOT_PURGED_CLAUSE = "t.$USER_PURGED IS FALSE"

    const val NOT_SERVICE_ACCOUNT = "t.$USER_TYPE <> 'SERVICE_ACCOUNT'"

    const val USER_TEXT_SEARCH_CLAUSE = """
        (
            :textSearched IS NULL OR
                UNACCENT(t.$USER_FIRST_NAME) ILIKE '%' || UNACCENT(:textSearched) || '%' OR UNACCENT(t.$USER_LAST_NAME) ILIKE '%' || UNACCENT(:textSearched) || '%' OR UNACCENT(t.$USER_EMAIL) ILIKE '%' || UNACCENT(:textSearched) || '%'
        )
    """
}
