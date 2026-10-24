package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.ProjectOptionValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

/**
 * Purpose: Bean validation constraint on the options of a project.
 * Scope: Marks the option list for the dependency validator.
 * Limits: Declaration only.
 */
@Repeatable
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = [ProjectOptionValidator::class])
annotation class ProjectOptionDependencies(
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
