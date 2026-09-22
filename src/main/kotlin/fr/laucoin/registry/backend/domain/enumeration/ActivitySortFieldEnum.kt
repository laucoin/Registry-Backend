package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/projects/{projectId}/activities`. */
enum class ActivitySortFieldEnum {
	NAME,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
	START_AVAILABILITY_DATE,
}
