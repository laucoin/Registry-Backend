package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.ProjectOptionDependencies
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.http.HttpStatus.BAD_REQUEST

/**
 * Objectif : Validateur de la contrainte ProjectOptionDependencies.
 * Périmètre : Répond 400 lorsqu'une option activée n'a pas l'une des options dont elle dépend.
 * Limites : Ne vérifie pas la permission d'activer une option.
 */
class ProjectOptionValidator: ConstraintValidator<ProjectOptionDependencies, List<ProjectOptionEnum>> {
	override fun isValid(options: List<ProjectOptionEnum>?, context: ConstraintValidatorContext): Boolean {
		options?.forEach { requireDependencies(it, options) }

		return true
	}

	private fun requireDependencies(option: ProjectOptionEnum, options: List<ProjectOptionEnum>) {
		val missingOptions = option.requiredOptions.minus(options)
		if (missingOptions.isEmpty()) return
		throw RegistryException(
			BAD_REQUEST,
			PROJECT_OPTIONS_MISSING,
			arrayListOf(option, missingOptions.joinToString(", "))
		)
	}
}
