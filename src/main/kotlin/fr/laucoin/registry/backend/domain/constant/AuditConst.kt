package fr.laucoin.registry.backend.domain.constant

/**
 * ADR 019 §5 — audit/correlation transport constants and the outcome
 * vocabulary stamped on every audit entry. The audited action vocabulary
 * itself lives in
 * [fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum].
 */
object AuditConst {
	const val CORRELATION_ID_HEADER = "X-Correlation-Id"
	const val CORRELATION_ID_CONTEXT_KEY = "registry.correlationId"
	const val OUTCOME_SUCCESS = "SUCCESS"
	const val OUTCOME_FAILURE_PREFIX = "FAILURE:"
	const val OUTCOME_CANCELLED = "CANCELLED"
}
