package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.DateDefinedForTime
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects

/**
 * Purpose: Validator of the DateDefinedForTime constraint.
 * Scope: Fails when the time field is defined without its date.
 * Limits: Reads the fields by reflection; no other check.
 */
class DateDefinedForTimeValidator: GenericValidator<DateDefinedForTime, CustomDateTimeWriterDto>() {
	override fun isValid(value: CustomDateTimeWriterDto, context: ConstraintValidatorContext): Boolean {
		return when {
			Objects.isNull(value.date) -> Objects.isNull(value.time)
			else -> true
		}
	}
}
