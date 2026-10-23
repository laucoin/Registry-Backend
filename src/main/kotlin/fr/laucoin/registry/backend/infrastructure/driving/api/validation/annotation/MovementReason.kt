package fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator.MovementReasonValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Purpose: Bean validation constraint on the reason of a movement.
 * Scope: Marks a movement body for the type, reason and activity compatibility validator.
 * Limits: Declaration only.
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
