package fr.laucoin.registry.backend.domain.enumeration

/**
 * Fields an Alert collection may be sorted by (API v2 sort grammar, ADR 017).
 * `paramName` is the camelCase name exposed in the query parameter.
 */
enum class AlertSortFieldEnum(val paramName: String) {
	DATE_TIME("dateTime"),
	TITLE("title"),
	STATUS("status");

	companion object {
		fun fromParamName(paramName: String): AlertSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
