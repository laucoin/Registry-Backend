package fr.laucoin.registry.backend.domain.constant

object AuditConst {
	const val CORRELATION_ID_HEADER = "X-Correlation-Id"
	const val CORRELATION_ID_CONTEXT_KEY = "registry.correlationId"
	const val OUTCOME_SUCCESS = "SUCCESS"
	const val OUTCOME_FAILURE_PREFIX = "FAILURE:"
	const val OUTCOME_CANCELLED = "CANCELLED"
}
