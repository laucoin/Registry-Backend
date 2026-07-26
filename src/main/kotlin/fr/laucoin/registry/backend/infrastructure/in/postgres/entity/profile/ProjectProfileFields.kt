package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_NAME

object ProjectProfileFields {
	const val PROJECT_PROFILE_TABLE = "tb_project_profile"

	private const val USER_PREFIX = "user_"
	const val PROJECT_PROFILE_USER_ID = "$USER_PREFIX$ID"
	const val PROJECT_PROFILE_USER_FIRST_NAME = "$USER_PREFIX$USER_FIRST_NAME"
	const val PROJECT_PROFILE_USER_LAST_NAME = "$USER_PREFIX$USER_LAST_NAME"
	const val PROJECT_PROFILE_USER_EMAIL = "$USER_PREFIX$USER_EMAIL"
	const val PROJECT_PROFILE_USER_LAST_LOGIN = "$USER_PREFIX$USER_LAST_LOGIN"

	const val PROJECT_PROFILE_ROLE = "role"
	const val PROJECT_PROFILE_STATUS = "status"
	const val PROJECT_PROFILE_START_ACCESS_DATE = "start_access_date"
	const val PROJECT_PROFILE_START_ACCESS_TIME = "start_access_time"
	const val PROJECT_PROFILE_END_ACCESS_DATE = "end_access_date"
	const val PROJECT_PROFILE_END_ACCESS_TIME = "end_access_time"
	const val PROJECT_PROFILE_FAVORITE = "favorite"

	const val ROLE_COUNT = "role_count"
}
