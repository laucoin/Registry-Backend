package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.ValidEmailsValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

/**
 * Objectif : Contrainte de bean validation exigeant que chaque élément d'une liste soit un email valide.
 * Périmètre : Porte le message d'erreur de la contrainte.
 * Limites : Déclaration uniquement.
 */
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = [ValidEmailsValidator::class])
annotation class ValidEmails(
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
