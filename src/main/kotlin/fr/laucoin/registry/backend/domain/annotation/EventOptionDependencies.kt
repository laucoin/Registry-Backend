package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.validator.EventOptionValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = [EventOptionValidator::class])
annotation class EventOptionDependencies(
    val message: String,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)
