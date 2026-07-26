package fr.laucoin.registry.backend.domain.enumeration

/**
 * Fields an Activity collection may be sorted by (API v2 sort grammar, ADR 017).
 * `paramName` is the camelCase name exposed in the query parameter.
 */
enum class ActivitySortFieldEnum(val paramName: String) {
	NAME("name"),
	DURATION("duration"),
	START_AVAILABILITY_DATE("startAvailabilityDate"),
	END_AVAILABILITY_DATE("endAvailabilityDate");

	companion object {
		fun fromParamName(paramName: String): ActivitySortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
