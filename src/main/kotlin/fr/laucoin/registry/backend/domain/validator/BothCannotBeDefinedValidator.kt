package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.BothCannotBeDefined
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class BothCannotBeDefinedValidator: ConstraintValidator<BothCannotBeDefined, Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private lateinit var firstField: String
    private lateinit var secondField: String

    override fun initialize(constraintAnnotation: BothCannotBeDefined) {
        firstField = constraintAnnotation.first
        secondField = constraintAnnotation.second
    }

    override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
        val properties = value::class.memberProperties
        val firstFieldProperty = properties.firstOrNull { it.name == firstField }
        val secondFieldProperty = properties.firstOrNull { it.name == secondField }

        if (Objects.isNull(firstFieldProperty) || Objects.isNull(secondFieldProperty)) {
            val exception = RegistryException(INTERNAL_SERVER_ERROR, NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME)
            log.error(
                "One of the given field names ({}, {}) don't exist for the object ({})",
                firstField,
                secondField,
                value,
                exception
            )
            throw exception
        }

        val firstValue = (firstFieldProperty as KProperty1<*, *>).getter.call(value)
        val secondValue = (secondFieldProperty as KProperty1<*, *>).getter.call(value)

        return ! (Objects.nonNull(firstValue) && Objects.nonNull(secondValue))
    }
}
