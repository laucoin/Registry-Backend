package fr.laucoin.registry.backend.domain.enumeration

/**
 * Fields a Communication collection may be sorted by (API v2 sort grammar, ADR 017).
 * `paramName` is the camelCase name exposed in the query parameter.
 */
enum class CommunicationSortFieldEnum(val paramName: String) {
	DATE_TIME("dateTime"),
	MESSAGE("message");

	companion object {
		fun fromParamName(paramName: String): CommunicationSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
