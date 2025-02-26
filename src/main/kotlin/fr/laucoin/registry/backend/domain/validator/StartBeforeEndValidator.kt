package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.COMPARING_WRONG_PARAMETER_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME
import fr.laucoin.registry.backend.domain.extension.DateExt.isBeforeOrEqual
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CustomDateTimeWriterDto
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class StartBeforeEndValidator: ConstraintValidator<StartBeforeEnd, Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private lateinit var startField: String
    private lateinit var endField: String

    override fun initialize(constraintAnnotation: StartBeforeEnd) {
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
            startValue is ZonedDateTime && endValue is ZonedDateTime -> startValue.isBefore(endValue)
            startValue is LocalDate && endValue is LocalDate -> startValue.isBefore(endValue)
            startValue is CustomDateTimeWriterDto && endValue is CustomDateTimeWriterDto -> {
                val startDateTime = CustomDateTimeModel(startValue.date !!, startValue.time)
                val endDateTime = CustomDateTimeModel(endValue.date !!, endValue.time)
                startDateTime.isBeforeOrEqual(endDateTime)
            }

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
