package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/users`. */
enum class UserSortFieldEnum {
	FIRST_NAME,
	LAST_NAME,
	EMAIL,
	LAST_LOGIN,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
}
