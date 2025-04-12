package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.DateDefinedForTime
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class DateDefinedForTimeValidator: ConstraintValidator<DateDefinedForTime, Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private lateinit var dateField: String
    private lateinit var timeField: String

    override fun initialize(constraintAnnotation: DateDefinedForTime) {
        dateField = constraintAnnotation.dateField
        timeField = constraintAnnotation.timeField
    }

    override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
        val properties = value::class.memberProperties
        val dateFieldProperty = properties.firstOrNull { it.name == dateField }
        val timeFieldProperty = properties.firstOrNull { it.name == timeField }

        if (Objects.isNull(dateFieldProperty) || Objects.isNull(timeFieldProperty)) {
            val exception = RegistryException(INTERNAL_SERVER_ERROR, NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME)
            log.error(
                "One of the given field names ({}, {}) don't exist for the object ({})",
                dateField,
                timeField,
                value,
                exception
            )
            throw exception
        }

        val dateValue = (dateFieldProperty as KProperty1<*, *>).getter.call(value)
        val timeValue = (timeFieldProperty as KProperty1<*, *>).getter.call(value)

        return (
                Objects.isNull(dateValue) && Objects.isNull(timeValue)
                || Objects.nonNull(dateValue) && Objects.nonNull(timeValue)
                || Objects.nonNull(dateValue) && Objects.isNull(timeValue)
               )
    }
}
