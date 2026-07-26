package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * ADR 019 §1 — marks an endpoint as rate limited. Declared on the controller
 * contract method so the behaviour is visible where the endpoint is defined;
 * [fr.laucoin.registry.backend.domain.handler.RateLimitHandler] enforces it.
 *
 * [whenParamPresent] restricts enforcement to requests carrying at least one
 * of the listed query parameters — e.g. a list endpoint that is only expensive
 * when a free-text `q` search is requested. Empty means always enforced.
 */
@Target(FUNCTION)
@Retention(RUNTIME)
annotation class RateLimited(
	val category: RateLimitCategoryEnum,
	val whenParamPresent: Array<String> = [],
)
