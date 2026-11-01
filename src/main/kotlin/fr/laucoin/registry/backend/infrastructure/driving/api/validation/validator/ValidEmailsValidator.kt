package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PROJECT_PROFILE_EMAIL_MAX
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.ValidEmails
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Objectif : Validateur de la contrainte ValidEmails.
 * Périmètre : Vérifie que chaque élément de la liste est un email de forme valide et de longueur bornée.
 * Limites : Une liste absente est acceptée ; la présence et la taille de la liste sont validées ailleurs.
 */
class ValidEmailsValidator: ConstraintValidator<ValidEmails, List<String?>?> {
	override fun isValid(value: List<String?>?, context: ConstraintValidatorContext): Boolean {
		return value == null || value.all { it != null && it.length <= PROJECT_PROFILE_EMAIL_MAX && EMAIL.matches(it) }
	}

	private companion object {
		val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
	}
}
