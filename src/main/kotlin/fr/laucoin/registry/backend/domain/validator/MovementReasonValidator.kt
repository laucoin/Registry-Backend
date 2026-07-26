package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.MovementReason
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects
import java.util.Optional
import java.util.UUID

class MovementReasonValidator : GenericValidator<MovementReason, Any>() {
	private lateinit var participantType: ParticipantTypeEnum
	private var hasActivity: Boolean = false

	override fun initialize(constraintAnnotation: MovementReason) {
		participantType = constraintAnnotation.participantType
		hasActivity = constraintAnnotation.hasActivity
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		val type: MovementTypeEnum? = Optional.ofNullable(extractValue("type", value))
			.map { MovementTypeEnum.valueOf(it.toString()) }
			.orElse(null)
		val reason: MovementReasonEnum? = Optional.ofNullable(extractValue("reason", value))
			.map { MovementReasonEnum.valueOf(it.toString()) }
			.orElse(null)
		val activityId: UUID? = extractActivityValue(value)

		return when {
			Objects.isNull(type) -> true
			Objects.isNull(reason) && participantType === GUEST -> type === OUT
			Objects.isNull(reason) && Objects.isNull(activityId) && participantType === REGISTERED -> type === IN
			else -> Objects.nonNull(activityId) || (reason!!.type == type && reason.participantType == participantType)
		}
	}

	private fun extractActivityValue(value: Any): UUID? {
		return if (hasActivity) Optional.ofNullable(extractValue("activityId", value))
			.map { UUID.fromString(it.toString()) }
			.orElse(null)
		else null
	}
}
