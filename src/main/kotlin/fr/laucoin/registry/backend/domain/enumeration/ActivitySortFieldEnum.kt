package fr.laucoin.registry.backend.domain.enumeration

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
