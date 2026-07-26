package fr.laucoin.registry.backend.domain.enumeration

/**
 * Fields a Vehicle collection may be sorted by (API v2 sort grammar, ADR 017).
 * `paramName` is the camelCase name exposed in the query parameter.
 */
enum class VehicleSortFieldEnum(val paramName: String) {
	LICENSE_PLATE("licensePlate"),
	BRAND("brand"),
	MODEL("model");

	companion object {
		fun fromParamName(paramName: String): VehicleSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
