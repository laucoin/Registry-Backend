package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/projects/{projectId}/vehicles`. */
enum class VehicleSortFieldEnum {
	BRAND,
	MODEL,
	LICENSE_PLATE,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
}
