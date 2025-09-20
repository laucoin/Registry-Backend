package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.ProjectOptionDependencies
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.http.HttpStatus.BAD_REQUEST

class ProjectOptionValidator: ConstraintValidator<ProjectOptionDependencies, List<ProjectOptionEnum>> {
	override fun isValid(options: List<ProjectOptionEnum>?, context: ConstraintValidatorContext): Boolean {
		val missingOptions = mutableSetOf<ProjectOptionEnum>()
		options?.forEach {
			missingOptions.addAll(it.requiredOptions.minus(options))
		}

		if (missingOptions.isNotEmpty()) {
			throw RegistryException(
				BAD_REQUEST,
				PROJECT_OPTIONS_MISSING,
				arrayListOf(missingOptions.joinToString(", "))
			)
		}

		return true
	}
}
