package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.AtLeastOneIsDefinedValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Objectif : Contrainte de bean validation exigeant qu'au moins un de deux champs soit défini.
 * Périmètre : Déclare les deux noms de champs vérifiés par son validateur.
 * Limites : Déclaration uniquement.
 */
@Repeatable
@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [AtLeastOneIsDefinedValidator::class])
annotation class AtLeastOneIsDefined(
	val first: String,
	val second: String,
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
