package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.ProjectOptionDependencies
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.http.HttpStatus.BAD_REQUEST

/**
 * Purpose: Validator of the ProjectOptionDependencies constraint.
 * Scope: Answers 400 when an enabled option lacks one of the options it depends on.
 * Limits: Does not check the permission to enable an option.
 */
class ProjectOptionValidator: ConstraintValidator<ProjectOptionDependencies, List<ProjectOptionEnum>> {
	override fun isValid(options: List<ProjectOptionEnum>?, context: ConstraintValidatorContext): Boolean {
		options?.forEach { option ->
			val missingOptions = option.requiredOptions.minus(options)
			if (missingOptions.isNotEmpty()) {
				throw RegistryException(
					BAD_REQUEST,
					PROJECT_OPTIONS_MISSING,
					arrayListOf(option, missingOptions.joinToString(", "))
				)
			}
		}

		return true
	}
}
