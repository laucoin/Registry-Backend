package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_OPTIONS

object PreferenceFields {
	const val PREFERENCE_TABLE = "tb_preferences"

	const val PREFERENCE_USER_ID = "user_id"
	const val PREFERENCE_THEME = "theme"
	const val PREFERENCE_LANGUAGE = "language"

	private const val SELECTED_PROFILE_PREFIX = "selected_profile_"
	private const val PROJECT_PREFIX = "project_"
	const val PREFERENCE_SELECTED_PROFILE_ID = "${SELECTED_PROFILE_PREFIX}$ID"
	const val PREFERENCE_SELECTED_PROFILE_PROJECT_ID = "${SELECTED_PROFILE_PREFIX}${PROJECT_PREFIX}$ID"
	const val PREFERENCE_SELECTED_PROFILE_PROJECT_NAME = "${SELECTED_PROFILE_PREFIX}${PROJECT_PREFIX}$PROJECT_NAME"
	const val PREFERENCE_SELECTED_PROFILE_PROJECT_START_DATE =
		"${SELECTED_PROFILE_PREFIX}${PROJECT_PREFIX}$PROJECT_BEGIN_DATE"
	const val PREFERENCE_SELECTED_PROFILE_PROJECT_START_TIME =
		"${SELECTED_PROFILE_PREFIX}${PROJECT_PREFIX}$PROJECT_BEGIN_TIME"
	const val PREFERENCE_SELECTED_PROFILE_PROJECT_END_DATE =
		"${SELECTED_PROFILE_PREFIX}${PROJECT_PREFIX}$PROJECT_END_DATE"
	const val PREFERENCE_SELECTED_PROFILE_PROJECT_END_TIME =
		"${SELECTED_PROFILE_PREFIX}${PROJECT_PREFIX}$PROJECT_END_TIME"
	const val PREFERENCE_SELECTED_PROFILE_PROJECT_OPTIONS =
		"${SELECTED_PROFILE_PREFIX}${PROJECT_PREFIX}$PROJECT_OPTIONS"
	const val PREFERENCE_SELECTED_PROFILE_ROLE = "${SELECTED_PROFILE_PREFIX}$PROJECT_PROFILE_ROLE"
	const val PREFERENCE_SELECTED_PROFILE_STATUS = "${SELECTED_PROFILE_PREFIX}$PROJECT_PROFILE_STATUS"
	const val PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE =
		"${SELECTED_PROFILE_PREFIX}$PROJECT_PROFILE_START_ACCESS_DATE"
	const val PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME =
		"${SELECTED_PROFILE_PREFIX}$PROJECT_PROFILE_START_ACCESS_TIME"
	const val PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE = "${SELECTED_PROFILE_PREFIX}$PROJECT_PROFILE_END_ACCESS_DATE"
	const val PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME = "${SELECTED_PROFILE_PREFIX}$PROJECT_PROFILE_END_ACCESS_TIME"
}
