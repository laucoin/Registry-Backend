package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.MovementReason
import jakarta.validation.ConstraintValidatorContext
import java.util.Optional
import java.util.UUID

/**
 * Objectif : Validateur de la contrainte MovementReason.
 * Périmètre : Vérifie la compatibilité entre type de mouvement, motif, type de participant et activité.
 * Limites : Ne vérifie pas que l'activité existe.
 */
class MovementReasonValidator: GenericValidator<MovementReason, Any>() {
	private lateinit var participantType: ParticipantTypeEnum
	private var hasActivity: Boolean = false

	override fun initialize(constraintAnnotation: MovementReason) {
		participantType = constraintAnnotation.participantType
		hasActivity = constraintAnnotation.hasActivity
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		val type: MovementTypeEnum = extractEnum<MovementTypeEnum>("type", value) ?: return true
		val reason: MovementReasonEnum? = extractEnum<MovementReasonEnum>("reason", value)

		return isCoherent(type, reason, extractActivityValue(value))
	}

	private fun isCoherent(type: MovementTypeEnum, reason: MovementReasonEnum?, activityId: UUID?): Boolean {
		return when {
			reason == null && participantType === GUEST -> type === OUT
			reason == null && activityId == null && participantType === REGISTERED -> type === IN
			else -> activityId != null || matches(reason!!, type)
		}
	}

	private fun matches(reason: MovementReasonEnum, type: MovementTypeEnum): Boolean {
		return reason.type == type && reason.participantType == participantType
	}

	private inline fun <reified T : Enum<T>> extractEnum(field: String, value: Any): T? {
		return extractValue(field, value)?.let { enumValueOf<T>(it.toString()) }
	}

	private fun extractActivityValue(value: Any): UUID? {
		return if (hasActivity) Optional.ofNullable(extractValue("activityId", value))
			.map { UUID.fromString(it.toString()) }
			.orElse(null)
		else null
	}
}
