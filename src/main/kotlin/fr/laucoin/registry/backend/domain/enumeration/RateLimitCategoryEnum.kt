package fr.laucoin.registry.backend.domain.enumeration

/**
 * The rate-limited operation categories. Each category has its own
 * deploy-tunable capacity/period (capacity 0 disables the category).
 */
enum class RateLimitCategoryEnum {
	SENSITIVE,
	SEARCH,
}
