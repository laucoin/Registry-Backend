package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/projects`. */
enum class ProjectSortFieldEnum {
	NAME,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
}
