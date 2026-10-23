package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.BothCannotBeDefinedValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Purpose: Bean validation constraint forbidding two fields to be defined together.
 * Scope: Declares the two field names checked by its validator.
 * Limits: Declaration only.
 */
@Repeatable
@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [BothCannotBeDefinedValidator::class])
annotation class BothCannotBeDefined(
	val first: String,
	val second: String,
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
