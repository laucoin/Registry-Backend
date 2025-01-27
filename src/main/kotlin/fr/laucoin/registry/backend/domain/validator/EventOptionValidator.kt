package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.EventOptionDependencies
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.Companion.missingOptions
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects
import org.springframework.http.HttpStatus.BAD_REQUEST

class EventOptionValidator: ConstraintValidator<EventOptionDependencies, List<EventOptionEnum>> {
    override fun isValid(options: List<EventOptionEnum>?, context: ConstraintValidatorContext): Boolean {
        val missing: Pair<EventOptionEnum, List<EventOptionEnum>>? = options?.missingOptions()

        if (Objects.nonNull(missing)) {
            throw RegistryException(
                BAD_REQUEST,
                EVENT_OPTIONS_MISSING,
                arrayListOf(missing !!.first.name, missing.second.joinToString(", "))
            )
        }

        return Objects.isNull(missing)
    }
}
