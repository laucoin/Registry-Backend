package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.EventOptionDependencies
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.Companion.missingOptions
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class EventOptionValidator: ConstraintValidator<EventOptionDependencies, List<EventOptionEnum>> {
    override fun isValid(options: List<EventOptionEnum>?, context: ConstraintValidatorContext): Boolean {
        return when {
            options?.missingOptions()?.isNotEmpty() == true -> false
            else -> true
        }
    }
}
