package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.MinUpperMaxValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Objectif : Contrainte de bean validation exigeant un minimum non supérieur à son maximum.
 * Périmètre : Déclare les deux noms de champs numériques vérifiés par son validateur.
 * Limites : Déclaration uniquement.
 */
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
