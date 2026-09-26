package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/projects/{projectId}/profiles`. */
enum class ProjectProfileSortFieldEnum {
	USER_LAST_NAME,
	USER_FIRST_NAME,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
}
