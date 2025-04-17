package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.validator.MovementReasonValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [MovementReasonValidator::class])
annotation class MovementReason(
    val type: String,
    val reason: String,
    val message: String,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)
