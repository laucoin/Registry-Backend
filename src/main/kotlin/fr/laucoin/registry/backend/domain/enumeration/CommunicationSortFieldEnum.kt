package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/projects/{projectId}/communications`. */
enum class CommunicationSortFieldEnum {
	DATE_TIME,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
}
