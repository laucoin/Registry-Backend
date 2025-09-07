package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UNKNOWN_ERROR
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IRegistryControllerAdvice
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ErrorDto
import java.util.Locale
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

@RestControllerAdvice
class RegistryControllerAdvice(
    private val translateService: ITranslateService,
) : IRegistryControllerAdvice, LoggerService() {
    override fun handleRegistryException(exception: RegistryException): Mono<ResponseEntity<ErrorDto>> {
        return buildError(exception.status, exception.code, exception.args?.toArray())
    }

    override fun handleWebExchangeBindException(exception: WebExchangeBindException): Mono<ResponseEntity<ErrorDto>> {
        val error = exception.allErrors.first()
        return buildError(
            status = BAD_REQUEST,
            code = error.defaultMessage!!,
            args = error.arguments ?: emptyArray(),
        )
    }

    override fun handleServerWebInputException(exception: ServerWebInputException): Mono<ResponseEntity<ErrorDto>> {
        return buildError(
            status = BAD_REQUEST,
            code = PARAMETER_TYPE_MISMATCH,
            args = arrayOf(exception.cause).filterNotNull().toTypedArray(),
        )
    }

    override fun handleHandlerMethodValidationException(exception: HandlerMethodValidationException): Mono<ResponseEntity<ErrorDto>> {
        val error: ParameterValidationResult = exception.valueResults.first()
        return buildError(
            status = BAD_REQUEST,
            code = error.resolvableErrors.first().defaultMessage!!,
            args = arrayOf(error.argument).filterNotNull().toTypedArray(),
        )
    }

    override fun handleHandlerAuthorizationDeniedException(exception: AuthorizationDeniedException): Mono<ResponseEntity<ErrorDto>> {
        return buildError(
            status = FORBIDDEN,
            code = NOT_ENOUGH_PERMISSION,
        )
    }

    override fun handleResponseStatusException(exception: ResponseStatusException): Mono<ResponseEntity<ErrorDto>> {
        return buildError(
            status = HttpStatus.valueOf(exception.statusCode.value()),
            code = exception.statusCode.value().toString(),
        )
    }

    override fun handleException(exception: Exception): Mono<ResponseEntity<ErrorDto>> {
        log.error("An unexpected error occurred", exception)
        return buildError(
            status = INTERNAL_SERVER_ERROR,
            code = "500",
        )
    }

    private fun buildError(
        status: HttpStatus,
        code: String,
        args: Array<Any>? = null,
    ): Mono<ResponseEntity<ErrorDto>> {
        return Mono.deferContextual {
            val locale = it.get(Locale::class.java)
            val body = ErrorDto(
                statusCode = status.value(),
                statusName = status.name,
                code = code,
                title = translateService.getMessage(
                    code = "$ERROR_TITLE_PREFIX${status.value()}",
                    locale = locale,
                ),
                message = translateService.getMessage(
                    code = "$ERROR_MESSAGE_PREFIX$code",
                    args = args,
                    default = translateService.getMessage(
                        code = "$ERROR_MESSAGE_PREFIX$UNKNOWN_ERROR",
                        locale = locale,
                    ),
                    locale = locale
                ),
            )

            Mono.just(ResponseEntity.status(status).body(body))
        }
    }
}
