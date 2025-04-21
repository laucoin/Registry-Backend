package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.COMPARING_WRONG_PARAMETER_TYPE
import fr.laucoin.registry.backend.domain.extension.DateExt.isBeforeOrEqual
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CustomDateTimeWriterDto
import jakarta.validation.ConstraintValidatorContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Objects
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class StartBeforeEndValidator: GenericValidator<StartBeforeEnd, Any>() {
    private lateinit var startField: String
    private lateinit var endField: String

    override fun initialize(constraintAnnotation: StartBeforeEnd) {
        startField = constraintAnnotation.startField
        endField = constraintAnnotation.endField
    }

    override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
        val startValue = extractValue(startField, value)
        val endValue = extractValue(endField, value)

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
