package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.MovementGuestContent
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.extension.ListExt.isNotEmpty
import fr.laucoin.registry.backend.domain.extension.ListExt.isNullOrEmpty
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto
import jakarta.validation.ConstraintValidatorContext

class MovementGuestContentValidator: GenericValidator<MovementGuestContent, GuestMovementWriterDto>() {
	override fun isValid(value: GuestMovementWriterDto, context: ConstraintValidatorContext): Boolean {
		return if (value.type === IN) {
			value.content.isNullOrEmpty() && value.guests.isNotEmpty()
		} else {
			value.guests.isNullOrEmpty() && value.content.isNotEmpty()
		}
	}
}
