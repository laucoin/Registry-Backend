package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user

object UserFields {
	const val USER_TABLE = "tb_user"

	const val USER_OIDC_ID = "oidc_id"
	const val USER_TYPE = "type"
	const val USER_FIRST_NAME = "first_name"
	const val USER_LAST_NAME = "last_name"
	const val USER_EMAIL = "email"
	const val USER_ROLE = "role"
	const val USER_BIRTHDAY = "birthday"
	const val USER_LAST_LOGIN = "last_login"
	const val USER_PURGED = "purged"
	const val PREFERENCE_ID = "preference_id"
}
