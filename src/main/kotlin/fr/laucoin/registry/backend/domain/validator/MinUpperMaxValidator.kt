package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.MinUpperMax
import fr.laucoin.registry.backend.domain.constant.ErrorConst.COMPARING_WRONG_PARAMETER_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.Objects
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class MinUpperMaxValidator: ConstraintValidator<MinUpperMax, Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private lateinit var startField: String
    private lateinit var endField: String

    override fun initialize(constraintAnnotation: MinUpperMax) {
        startField = constraintAnnotation.startField
        endField = constraintAnnotation.endField
    }

    override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
        val properties = value::class.memberProperties
        val startFieldProperty = properties.firstOrNull { it.name == startField }
        val endFieldProperty = properties.firstOrNull { it.name == endField }

        if (Objects.isNull(startFieldProperty) || Objects.isNull(endFieldProperty)) {
            val exception = RegistryException(INTERNAL_SERVER_ERROR, NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME)
            log.error(
                "One of the given field names ({}, {}) don't exist for the object ({})",
                startField,
                endField,
                value,
                exception
            )
            throw exception
        }

        val startValue = (startFieldProperty as KProperty1<*, *>).getter.call(value)
        val endValue = (endFieldProperty as KProperty1<*, *>).getter.call(value)

        return when {
            startValue is Int && endValue is Int -> startValue <= endValue
            startValue is Double && endValue is Double -> startValue <= endValue
            startValue is Float && endValue is Float -> startValue <= endValue
            startValue is Long && endValue is Long -> startValue <= endValue
            Objects.isNull(startValue) || Objects.isNull(endValue) -> true
            else -> {
                val exception = RegistryException(INTERNAL_SERVER_ERROR, COMPARING_WRONG_PARAMETER_TYPE)
                log.error(
                    "The two fields ({}, {}) are not of the same type or the type is not supported (we support only nullable ZonedDateTime and ZonedDateTime).",
                    startValue,
                    endValue,
                    exception
                )
                throw exception
            }
        }
    }
}
