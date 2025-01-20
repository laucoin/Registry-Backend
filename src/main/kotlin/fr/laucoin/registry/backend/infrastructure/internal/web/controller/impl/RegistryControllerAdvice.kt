package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UNKNOWN_ERROR
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IRegistryControllerAdvice
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ErrorDto
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ServerWebInputException

@ControllerAdvice
class RegistryControllerAdvice: IRegistryControllerAdvice {
    override fun handleRegistryException(exception: RegistryException): ResponseEntity<ErrorDto> {
        return handleSecurityException(exception.status, exception.message)
    }

    override fun handleWebExchangeBindException(exception: WebExchangeBindException): ResponseEntity<ErrorDto> {
        val error = exception.allErrors.first()
        return handleSecurityException(
            status = BAD_REQUEST,
            code = error.defaultMessage !!,
            args = error.arguments ?: emptyArray(),
        )

    }

    override fun handleServerWebInputException(exception: ServerWebInputException): ResponseEntity<ErrorDto> {
        val error = exception.cause as TypeMismatchException
        return handleSecurityException(
            status = BAD_REQUEST,
            code = PARAMETER_TYPE_MISMATCH,
            args = arrayOf(error.value),
        )
    }

    override fun handleHandlerMethodValidationException(exception: HandlerMethodValidationException): ResponseEntity<ErrorDto> {
        val error: ParameterValidationResult = exception.valueResults.first()
        return handleSecurityException(
            status = BAD_REQUEST,
            code = error.resolvableErrors.first().defaultMessage !!,
            args = arrayOf(error.argument),
        )
    }

    override fun handleHandlerAuthorizationDeniedException(exception: AuthorizationDeniedException): ResponseEntity<ErrorDto> {
        return handleSecurityException(
            status = FORBIDDEN,
            code = NOT_ENOUGH_PERMISSION,
        )
    }

    override fun handleException(exception: Exception): ResponseEntity<ErrorDto> {
        return handleSecurityException(
            status = INTERNAL_SERVER_ERROR,
            code = UNKNOWN_ERROR,
            args = arrayOf(exception.message),
        )
    }

    private fun handleSecurityException(
        status: HttpStatus,
        code: String,
        args: Array<Any?> = emptyArray(),
    ): ResponseEntity<ErrorDto> {
        return ResponseEntity(
            ErrorDto(
                statusCode = status.value(),
                statusName = status.name,
                code = code,
                message = code,
            ),
            status
        )
    }
}
