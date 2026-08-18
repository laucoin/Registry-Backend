package fr.laucoin.registry.backend.domain.enumeration

enum class VehicleSortFieldEnum(val paramName: String) {
	LICENSE_PLATE("licensePlate"),
	BRAND("brand"),
	MODEL("model");

	companion object {
		fun fromParamName(paramName: String): VehicleSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
