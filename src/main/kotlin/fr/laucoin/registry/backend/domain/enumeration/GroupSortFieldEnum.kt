package fr.laucoin.registry.backend.domain.enumeration

enum class GroupSortFieldEnum(val paramName: String) {
	NAME("name"),
	START_AVAILABILITY_DATE("startAvailabilityDate"),
	END_AVAILABILITY_DATE("endAvailabilityDate");

	companion object {
		fun fromParamName(paramName: String): GroupSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
