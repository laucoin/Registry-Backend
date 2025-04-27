package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidator
import java.util.Objects
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

abstract class GenericValidator<A: Annotation?, T>: ConstraintValidator<A, T> {
    protected val log = LoggerFactory.getLogger(this::class.java)

    protected fun extractValue(fieldName: String, value: Any): Any? {
        val properties = value::class.memberProperties
        val fieldProperty = properties.firstOrNull { it.name == fieldName }

        if (Objects.isNull(fieldProperty)) {
            val exception = RegistryException(INTERNAL_SERVER_ERROR, NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME)
            log.error(
                "The field name ({}) don't exist for the object ({})",
                fieldName,
                value,
                exception
            )
            throw exception
        }

        return (fieldProperty as KProperty1<*, *>).getter.call(value)
    }
}
