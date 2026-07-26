package fr.laucoin.registry.backend.domain.enumeration

/**
 * ADR 019 §5 — the audit trail vocabulary, one entry per privileged action.
 * Only privileged/destructive administrative actions are audited (RBAC and
 * access changes, deletes, anonymization): they are the security-relevant,
 * low-volume events the trail exists for. Regular CRUD stays out — it is
 * high-volume, already access-controlled, and would drown the trail.
 */
enum class AuditActionEnum {
	USER_BLOCK,
	USER_UNBLOCK,
	USER_DELETE,
	USER_ROLE_UPDATE,

	PROJECT_DISABLE,
	PROJECT_ENABLE,
	PROJECT_DELETE,

	PROFILE_BLOCK,
	PROFILE_UNBLOCK,
	PROFILE_DELETE,
	PROFILE_ROLE_UPDATE,

	MEMBERSHIP_SUPPORT_ACCESS,
	MEMBERSHIP_STATUS_UPDATE,
	MEMBERSHIP_REVOKE,
}
