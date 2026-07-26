package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.AtLeastOneIsDefined
import fr.laucoin.registry.backend.domain.extension.ListExt.isIterable
import fr.laucoin.registry.backend.domain.extension.ListExt.isNotEmpty
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects

class AtLeastOneIsDefinedValidator : GenericValidator<AtLeastOneIsDefined, Any>() {
	private lateinit var firstField: String
	private lateinit var secondField: String

	override fun initialize(constraintAnnotation: AtLeastOneIsDefined) {
		firstField = constraintAnnotation.first
		secondField = constraintAnnotation.second
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		val firstValue = extractValue(firstField, value)
		val secondValue = extractValue(secondField, value)

		val firstValueIsIterable = (firstValue.isIterable() || Objects.isNull(firstValue))
		val secondValueIsIterable = (secondValue.isIterable() || Objects.isNull(secondValue))
		return if (firstValueIsIterable && secondValueIsIterable) {
			firstValue.isNotEmpty() || secondValue.isNotEmpty()
		} else {
			Objects.nonNull(firstValue) || Objects.nonNull(secondValue)
		}
	}
}
