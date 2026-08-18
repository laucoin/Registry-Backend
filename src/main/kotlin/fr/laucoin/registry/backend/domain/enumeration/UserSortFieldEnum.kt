package fr.laucoin.registry.backend.domain.enumeration

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
