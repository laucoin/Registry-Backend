package fr.laucoin.registry.backend.domain.enumeration

/**
 * ADR 019 §1 — the rate-limited operation categories. Each category has its
 * own deploy-tunable capacity/period (capacity 0 disables the category).
 */
enum class RateLimitCategoryEnum {
	/** Mutating state transitions (block/disable/…), deletes and purge jobs. */
	SENSITIVE,

	/** The trigram-backed pickers and free-text searches. */
	SEARCH,
}
