package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.MovementReasonValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Objectif : Contrainte de bean validation sur le motif d'un mouvement.
 * Périmètre : Marque un corps de mouvement pour le validateur de compatibilité entre type, motif et activité.
 * Limites : Déclaration uniquement.
 */
@Repeatable
@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [MovementReasonValidator::class])
annotation class MovementReason(
	val participantType: ParticipantTypeEnum,
	val hasActivity: Boolean = false,
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
