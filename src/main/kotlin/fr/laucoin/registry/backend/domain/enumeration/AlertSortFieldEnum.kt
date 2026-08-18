package fr.laucoin.registry.backend.domain.enumeration

enum class AlertSortFieldEnum(val paramName: String) {
	DATE_TIME("dateTime"),
	TITLE("title"),
	STATUS("status");

	companion object {
		fun fromParamName(paramName: String): AlertSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
