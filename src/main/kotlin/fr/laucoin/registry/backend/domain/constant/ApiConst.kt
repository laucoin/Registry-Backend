package fr.laucoin.registry.backend.domain.constant

/**
 * Shared bounds for non-paginated collection endpoints: every list endpoint
 * must be bounded, so dashboard-style collections take a `limit` capped at the
 * paginated endpoints' maximum page size, which is also the default so callers
 * that never paginated (v1) keep their practical behavior.
 */
object ApiConst {
	const val DEFAULT_COLLECTION_LIMIT = 200
	const val DEFAULT_COLLECTION_LIMIT_PARAM = "$DEFAULT_COLLECTION_LIMIT"
}
