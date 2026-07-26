package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.validator.AtLeastOneNonEmptyValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Class-level constraint satisfied when at least one of the named collection
 * fields is non-null and non-empty. Used to require "userIds OR emails" on the
 * project-profile creation body.
 */
@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [AtLeastOneNonEmptyValidator::class])
annotation class AtLeastOneNonEmpty(
	val fields: Array<String>,
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
