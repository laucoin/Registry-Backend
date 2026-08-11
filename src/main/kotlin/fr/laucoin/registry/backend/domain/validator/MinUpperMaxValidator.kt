package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.MinUpperMax
import fr.laucoin.registry.backend.domain.constant.ErrorConst.COMPARING_WRONG_PARAMETER_TYPE
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidatorContext
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import java.util.Objects

class MinUpperMaxValidator : GenericValidator<MinUpperMax, Any>() {
	private lateinit var startField: String
	private lateinit var endField: String

	override fun initialize(constraintAnnotation: MinUpperMax) {
		startField = constraintAnnotation.startField
		endField = constraintAnnotation.endField
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		val startValue = extractValue(startField, value)
		val endValue = extractValue(endField, value)

		return when {
			startValue is Int && endValue is Int -> startValue <= endValue
			startValue is Double && endValue is Double -> startValue <= endValue
			startValue is Float && endValue is Float -> startValue <= endValue
			startValue is Long && endValue is Long -> startValue <= endValue
			Objects.isNull(startValue) || Objects.isNull(endValue) -> true
			else -> {
				val exception = RegistryException(
					INTERNAL_SERVER_ERROR,
					COMPARING_WRONG_PARAMETER_TYPE,
					arrayListOf(startValue, endValue)
				)
				log.error(
					"The two fields ({}, {}) are not of the same type or the type is not supported (we support only nullable ZonedDateTime and ZonedDateTime).",
					startValue,
					endValue,
					exception
				)
				throw exception
			}
		}
	}
}
