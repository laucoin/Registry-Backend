package fr.laucoin.registry.backend.domain.constant

/**
 * The single upper bound on how much of a collection one request may return.
 * Paginated endpoints enforce it as the maximum `size`; the dashboard-style
 * endpoints that take a plain `limit` reuse the same ceiling, which is also
 * their default so callers that never paginated (v1) keep their behaviour.
 * [MAX_PAGE_SIZE] is the same number typed for Bean Validation's `long` bound.
 */
object ApiConst {
	const val DEFAULT_COLLECTION_LIMIT = 200
	const val DEFAULT_COLLECTION_LIMIT_PARAM = "$DEFAULT_COLLECTION_LIMIT"
	const val MAX_PAGE_SIZE = 200L
	const val DEFAULT_PAGE_NUMBER = 0
	const val DEFAULT_PAGE_SIZE = 20
}
