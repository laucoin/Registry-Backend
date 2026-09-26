package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/projects/{projectId}/groups`. */
enum class GroupSortFieldEnum {
	NAME,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
}
