package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.ValidEmails
import jakarta.validation.ConstraintValidatorContext

class ValidEmailsValidator : GenericValidator<ValidEmails, Any>() {
	private lateinit var field: String

	override fun initialize(constraintAnnotation: ValidEmails) {
		field = constraintAnnotation.field
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		val fieldValue = extractValue(field, value)
		if (fieldValue !is Collection<*>) return true

		return fieldValue.all { it is String && EMAIL_REGEX.matches(it) }
	}

	private companion object {
		private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
	}
}
