package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.BothCannotBeDefined
import fr.laucoin.registry.backend.domain.extension.ListExt.isIterable
import fr.laucoin.registry.backend.domain.extension.ListExt.isNullOrEmpty
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects

/**
 * Purpose: Validator of the BothCannotBeDefined constraint.
 * Scope: Fails when both configured fields are defined.
 * Limits: Reads the fields by reflection; no other check.
 */
class BothCannotBeDefinedValidator: GenericValidator<BothCannotBeDefined, Any>() {
	private lateinit var firstField: String
	private lateinit var secondField: String

	override fun initialize(constraintAnnotation: BothCannotBeDefined) {
		firstField = constraintAnnotation.first
		secondField = constraintAnnotation.second
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		val firstValue = extractValue(firstField, value)
		val secondValue = extractValue(secondField, value)

		return if (firstValue.isIterable() && secondValue.isIterable()) {
			firstValue.isNullOrEmpty() || secondValue.isNullOrEmpty()
		} else {
			Objects.isNull(firstValue) || Objects.isNull(secondValue)
		}
	}
}
