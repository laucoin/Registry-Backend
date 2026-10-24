package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.DateDefinedForTime
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects

/**
 * Objectif : Validateur de la contrainte DateDefinedForTime.
 * Périmètre : Échoue lorsque le champ heure est défini sans sa date.
 * Limites : Lit les champs par réflexion ; aucune autre vérification.
 */
class DateDefinedForTimeValidator: GenericValidator<DateDefinedForTime, CustomDateTimeWriterDto>() {
	override fun isValid(value: CustomDateTimeWriterDto, context: ConstraintValidatorContext): Boolean {
		return when {
			Objects.isNull(value.date) -> Objects.isNull(value.time)
			else -> true
		}
	}
}
