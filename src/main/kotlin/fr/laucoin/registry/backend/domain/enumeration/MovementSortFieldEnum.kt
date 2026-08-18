package fr.laucoin.registry.backend.domain.enumeration

enum class MovementSortFieldEnum(val paramName: String) {
	DATE_TIME("dateTime"),
	TYPE("type"),
	REASON("reason");

	companion object {
		fun fromParamName(paramName: String): MovementSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
