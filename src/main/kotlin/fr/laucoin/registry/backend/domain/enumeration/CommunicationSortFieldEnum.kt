package fr.laucoin.registry.backend.domain.enumeration

enum class CommunicationSortFieldEnum(val paramName: String) {
	DATE_TIME("dateTime"),
	MESSAGE("message");

	companion object {
		fun fromParamName(paramName: String): CommunicationSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
