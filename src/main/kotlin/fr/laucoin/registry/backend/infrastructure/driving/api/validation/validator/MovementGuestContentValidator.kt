package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.MovementGuestContent
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.extension.ListExt.isNotEmpty
import fr.laucoin.registry.backend.domain.extension.ListExt.isNullOrEmpty
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import jakarta.validation.ConstraintValidatorContext

/**
 * Purpose: Validator of the MovementGuestContent constraint.
 * Scope: Requires guests only on an IN movement and existing participants only on an OUT movement.
 * Limits: Does not check that the referenced participants exist.
 */
class MovementGuestContentValidator: GenericValidator<MovementGuestContent, GuestMovementWriterDto>() {
	override fun isValid(value: GuestMovementWriterDto, context: ConstraintValidatorContext): Boolean {
		return if (value.type === IN) {
			value.content.isNullOrEmpty() && value.guests.isNotEmpty()
		} else {
			value.guests.isNullOrEmpty() && value.content.isNotEmpty()
		}
	}
}
