package fr.laucoin.registry.backend.domain.enumeration

/**
 * Fields a Group collection may be sorted by (API v2 sort grammar, ADR 017).
 * `paramName` is the camelCase name exposed in the query parameter.
 */
enum class GroupSortFieldEnum(val paramName: String) {
	NAME("name"),
	START_AVAILABILITY_DATE("startAvailabilityDate"),
	END_AVAILABILITY_DATE("endAvailabilityDate");

	companion object {
		fun fromParamName(paramName: String): GroupSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
