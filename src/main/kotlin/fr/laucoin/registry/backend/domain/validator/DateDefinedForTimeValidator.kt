package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.DateDefinedForTime
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects

class DateDefinedForTimeValidator: GenericValidator<DateDefinedForTime, CustomDateTimeWriterDto>() {
	override fun isValid(value: CustomDateTimeWriterDto, context: ConstraintValidatorContext): Boolean {
		return when {
			Objects.isNull(value.date) -> Objects.isNull(value.time)
			else -> true
		}
	}
}
