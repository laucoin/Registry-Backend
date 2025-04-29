package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.validator.ProjectOptionValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

@Repeatable
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = [ProjectOptionValidator::class])
annotation class ProjectOptionDependencies(
    val message: String,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)
