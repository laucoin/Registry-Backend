package fr.laucoin.registry.backend.domain.enumeration

/**
 * Fields a Project collection may be sorted by (API v2 sort grammar, ADR 017).
 * `paramName` is the camelCase name exposed in the query parameter.
 */
enum class ProjectSortFieldEnum(val paramName: String) {
	NAME("name"),
	BEGIN_DATE("beginDate"),
	END_DATE("endDate");

	companion object {
		fun fromParamName(paramName: String): ProjectSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
