package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.MinUpperMaxValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Purpose: Bean validation constraint requiring a minimum not above its maximum.
 * Scope: Declares the two numeric field names checked by its validator.
 * Limits: Declaration only.
 */
@Repeatable
@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [MinUpperMaxValidator::class])
annotation class MinUpperMax(
	val startField: String,
	val endField: String,
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
