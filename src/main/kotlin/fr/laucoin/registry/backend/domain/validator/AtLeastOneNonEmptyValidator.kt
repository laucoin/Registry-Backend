package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.AtLeastOneNonEmpty
import jakarta.validation.ConstraintValidatorContext

class AtLeastOneNonEmptyValidator : GenericValidator<AtLeastOneNonEmpty, Any>() {
	private lateinit var fields: Array<String>

	override fun initialize(constraintAnnotation: AtLeastOneNonEmpty) {
		fields = constraintAnnotation.fields
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		return fields.any { field ->
			val fieldValue = extractValue(field, value)
			fieldValue is Collection<*> && fieldValue.isNotEmpty()
		}
	}
}
