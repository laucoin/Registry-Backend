package fr.laucoin.registry.backend.domain.enumeration

/**
 * Fields a Movement collection may be sorted by (API v2 sort grammar, ADR 017).
 * `paramName` is the camelCase name exposed in the query parameter.
 */
enum class MovementSortFieldEnum(val paramName: String) {
	DATE_TIME("dateTime"),
	TYPE("type"),
	REASON("reason");

	companion object {
		fun fromParamName(paramName: String): MovementSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
