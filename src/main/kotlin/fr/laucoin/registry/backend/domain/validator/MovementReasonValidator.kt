package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.MovementReason
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class MovementReasonValidator: ConstraintValidator<MovementReason, Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private lateinit var typeField: String
    private lateinit var reasonField: String

    override fun initialize(constraintAnnotation: MovementReason) {
        typeField = constraintAnnotation.type
        reasonField = constraintAnnotation.reason
    }

    override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
        val properties = value::class.memberProperties
        val typeFieldProperty = properties.firstOrNull { it.name == typeField }
        val reasonFieldProperty = properties.firstOrNull { it.name == reasonField }

        if (Objects.isNull(typeFieldProperty) || Objects.isNull(reasonFieldProperty)) {
            val exception = RegistryException(INTERNAL_SERVER_ERROR, NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME)
            log.error(
                "One of the given field names ({}, {}) don't exist for the object ({})",
                typeFieldProperty,
                reasonFieldProperty,
                value,
                exception
            )
            throw exception
        }

        val typeValue = (typeFieldProperty as KProperty1<*, *>).getter.call(value)
        val reasonValue = (reasonFieldProperty as KProperty1<*, *>).getter.call(value)

        if (Objects.isNull(typeValue) || Objects.isNull(reasonValue)) {
            return true
        }

        return MovementReasonEnum.valueOf(reasonValue.toString()).type.name == typeValue.toString()
    }
}
