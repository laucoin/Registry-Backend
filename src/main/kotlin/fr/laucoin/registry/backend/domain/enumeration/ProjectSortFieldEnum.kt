package fr.laucoin.registry.backend.domain.enumeration

enum class ProjectSortFieldEnum(val paramName: String) {
	NAME("name"),
	BEGIN_DATE("beginDate"),
	END_DATE("endDate");

	companion object {
		fun fromParamName(paramName: String): ProjectSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
