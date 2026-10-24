package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.BothCannotBeDefinedValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Objectif : Contrainte de bean validation interdisant que deux champs soient définis ensemble.
 * Périmètre : Déclare les deux noms de champs vérifiés par son validateur.
 * Limites : Déclaration uniquement.
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
