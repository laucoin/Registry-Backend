package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_LANGUAGE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_START_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_START_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_THEME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.project.ProjectFields.PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.PREFERENCE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TYPE

object UserQueries {
    const val SELECT_PREFERENCES = """
        $PREFERENCE_TABLE.$ID AS $PREFERENCE_ID,
        $PREFERENCE_TABLE.$PREFERENCE_THEME,
        $PREFERENCE_TABLE.$PREFERENCE_LANGUAGE,
        $PREFERENCE_TABLE.$PREFERENCE_SELECTED_PROFILE_ID,
        $PROJECT_TABLE.$ID AS $PREFERENCE_SELECTED_PROFILE_PROJECT_ID,
        $PROJECT_TABLE.$PROJECT_NAME AS $PREFERENCE_SELECTED_PROFILE_PROJECT_NAME,
        $PROJECT_TABLE.$PROJECT_BEGIN_DATE AS $PREFERENCE_SELECTED_PROFILE_PROJECT_START_DATE,
        $PROJECT_TABLE.$PROJECT_BEGIN_TIME AS $PREFERENCE_SELECTED_PROFILE_PROJECT_START_TIME,
        $PROJECT_TABLE.$PROJECT_END_DATE AS $PREFERENCE_SELECTED_PROFILE_PROJECT_END_DATE,
        $PROJECT_TABLE.$PROJECT_END_TIME AS $PREFERENCE_SELECTED_PROFILE_PROJECT_END_TIME,
        $PROJECT_TABLE.$PROJECT_OPTIONS AS $PREFERENCE_SELECTED_PROFILE_PROJECT_OPTIONS,
        $PROJECT_PROFILE_TABLE.$PROJECT_PROFILE_ROLE AS $PREFERENCE_SELECTED_PROFILE_ROLE,
        $PROJECT_PROFILE_TABLE.$PROJECT_PROFILE_STATUS AS $PREFERENCE_SELECTED_PROFILE_STATUS,
        $PROJECT_PROFILE_TABLE.$PROJECT_PROFILE_START_ACCESS_DATE AS $PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE,
        $PROJECT_PROFILE_TABLE.$PROJECT_PROFILE_START_ACCESS_TIME AS $PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME,
        $PROJECT_PROFILE_TABLE.$PROJECT_PROFILE_END_ACCESS_DATE AS $PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE,
        $PROJECT_PROFILE_TABLE.$PROJECT_PROFILE_END_ACCESS_TIME AS $PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME
    """

    const val PREFERENCES_JOIN = """
        LEFT JOIN $PREFERENCE_TABLE ON t.$ID = $PREFERENCE_TABLE.$PREFERENCE_USER_ID AND $PREFERENCE_TABLE.$VISIBLE IS TRUE
        LEFT JOIN $PROJECT_PROFILE_TABLE ON $PREFERENCE_TABLE.$PREFERENCE_SELECTED_PROFILE_ID = $PROJECT_PROFILE_TABLE.$ID AND $PROJECT_PROFILE_TABLE.$VISIBLE IS TRUE
        LEFT JOIN $PROJECT_TABLE ON $PROJECT_PROFILE_TABLE.$LINKED_PROJECT_ID = $PROJECT_TABLE.$ID AND $PROJECT_TABLE.$VISIBLE IS TRUE
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
