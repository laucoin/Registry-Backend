package fr.laucoin.registry.backend.domain.enumeration

/**
 * The rate-limited v2 endpoint categories. Each has its own deploy-tunable
 * capacity/window in `registry.security.rate-limit.*` — see
 * [fr.laucoin.registry.backend.domain.handler.RateLimitHandler].
 */
enum class RateLimitCategoryEnum {
	/** Mutations: create/update/delete/disable/enable/block/unblock. */
	SENSITIVE,

	/** Expensive reads — list/typeahead endpoints, usually gated to when a free-text search is present. */
	SEARCH,
}
