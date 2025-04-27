package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.DateDefinedForTime
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects

class DateDefinedForTimeValidator: GenericValidator<DateDefinedForTime, Any>() {
    private lateinit var dateField: String
    private lateinit var timeField: String

    override fun initialize(constraintAnnotation: DateDefinedForTime) {
        dateField = constraintAnnotation.dateField
        timeField = constraintAnnotation.timeField
    }

    override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
        val dateValue = extractValue(dateField, value)
        val timeValue = extractValue(timeField, value)

        return (
                Objects.isNull(dateValue) && Objects.isNull(timeValue)
                || Objects.nonNull(dateValue) && Objects.nonNull(timeValue)
                || Objects.nonNull(dateValue) && Objects.isNull(timeValue)
               )
    }
}
