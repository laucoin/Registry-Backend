package fr.laucoin.registry.backend.domain.enumeration

/**
 * Fields a User collection may be sorted by (API v2 sort grammar, ADR 017).
 * `paramName` is the camelCase name exposed in the query parameter.
 */
enum class UserSortFieldEnum(val paramName: String) {
	FIRST_NAME("firstName"),
	LAST_NAME("lastName"),
	EMAIL("email"),
	ROLE("role"),
	LAST_LOGIN("lastLogin");

	companion object {
		fun fromParamName(paramName: String): UserSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
