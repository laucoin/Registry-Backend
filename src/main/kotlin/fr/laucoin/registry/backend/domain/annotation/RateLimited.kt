package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Marks a v2 controller-interface method as rate limited, enforced by
 * [fr.laucoin.registry.backend.domain.handler.RateLimitHandler]. Declared on
 * the contract interface next to `@PreAuthorize`/`@Operation` so the
 * behaviour is visible where the endpoint is defined.
 *
 * [whenParamPresent] restricts enforcement to requests carrying at least one
 * of the listed query parameters — e.g. a list endpoint that is only
 * expensive when a free-text `q` search is requested. Empty means always
 * enforced.
 */
@Target(FUNCTION)
@Retention(RUNTIME)
annotation class RateLimited(
	val category: RateLimitCategoryEnum,
	val whenParamPresent: Array<String> = [],
)
