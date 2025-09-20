package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.validator.MinUpperMaxValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

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
