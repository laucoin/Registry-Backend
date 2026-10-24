package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.ProjectOptionValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

/**
 * Objectif : Contrainte de bean validation sur les options d'un projet.
 * Périmètre : Marque la liste d'options pour le validateur de dépendances.
 * Limites : Déclaration uniquement.
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
