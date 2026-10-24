package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.MovementGuestContent
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.extension.ListExt.isNotEmpty
import fr.laucoin.registry.backend.domain.extension.ListExt.isNullOrEmpty
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import jakarta.validation.ConstraintValidatorContext

/**
 * Objectif : Validateur de la contrainte MovementGuestContent.
 * Périmètre : Exige des invités uniquement sur un mouvement IN et des participants existants uniquement sur un mouvement OUT.
 * Limites : Ne vérifie pas que les participants référencés existent.
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
