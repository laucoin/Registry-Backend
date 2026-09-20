package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/projects/{projectId}/alerts`. */
enum class AlertSortFieldEnum {
	DATE_TIME,
	TITLE,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
}
