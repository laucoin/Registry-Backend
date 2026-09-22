package fr.laucoin.registry.backend.domain.enumeration

/** Whitelisted `sort` values for `GET /api/v2/projects/{projectId}/participants`. */
enum class ParticipantSortFieldEnum {
	FIRST_NAME,
	LAST_NAME,
	BIRTHDAY,
	CREATED_DATE,
	LAST_MODIFIED_DATE,
}
