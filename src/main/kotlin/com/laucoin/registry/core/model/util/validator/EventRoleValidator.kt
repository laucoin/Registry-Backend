package com.laucoin.registry.core.model.util.validator

import com.laucoin.registry.core.adapter.SecurityProperties
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component

@Component
class EventRoleValidator(
    private val securityProperties: SecurityProperties
): ConstraintValidator<EventRoleExist, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        return securityProperties.profileRoles().contains(value)
    }
}
