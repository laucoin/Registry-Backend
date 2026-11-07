package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.StartBeforeEndValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Objectif : Contrainte de bean validation exigeant un début strictement antérieur à sa fin.
 * Périmètre : Déclare les noms des champs début et fin vérifiés par son validateur.
 * Limites : Déclaration uniquement.
 */
@Repeatable
@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [StartBeforeEndValidator::class])
annotation class StartBeforeEnd(
	val startField: String,
	val endField: String,
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
