package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.COMPARING_WRONG_PARAMETER_TYPE
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import jakarta.validation.ConstraintValidatorContext
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Objects

class StartBeforeEndValidator : GenericValidator<StartBeforeEnd, Any>() {
	private lateinit var startField: String
	private lateinit var endField: String

	override fun initialize(constraintAnnotation: StartBeforeEnd) {
		startField = constraintAnnotation.startField
		endField = constraintAnnotation.endField
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		val startValue = extractValue(startField, value)
		val endValue = extractValue(endField, value)

		return when {
			startValue is ZonedDateTime && endValue is ZonedDateTime -> startValue.isBefore(endValue)
			startValue is LocalDate && endValue is LocalDate -> startValue.isBefore(endValue)
			startValue is CustomDateTimeWriterDto && endValue is CustomDateTimeWriterDto -> isValid(
				startValue,
				endValue
			)

			Objects.isNull(startValue) || Objects.isNull(endValue) -> true
			else -> {
				val exception = RegistryException(
					INTERNAL_SERVER_ERROR,
					COMPARING_WRONG_PARAMETER_TYPE,
					arrayListOf(startValue, endValue)
				)
				log.error(
					"The two fields ({}, {}) are not of the same type or the type is not supported.",
					startValue,
					endValue,
					exception
				)
				throw exception
			}
		}
	}

	private fun isValid(startValue: CustomDateTimeWriterDto, endValue: CustomDateTimeWriterDto): Boolean {
		return when {
			startValue.date!!.isBefore(endValue.date) -> true
			startValue.date!!.isEqual(endValue.date) -> {
				val startTime = startValue.time ?: return true
				val endTime = endValue.time ?: return true
				startTime.isBefore(endTime)
			}

			else -> false
		}
	}
}
