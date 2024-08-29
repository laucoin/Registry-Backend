package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.config.GsonConfig
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UNKNOWN_ERROR
import fr.laucoin.registry.backend.domain.extension.StringExt.getStringBetween
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.Date
import java.util.Objects
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.validation.ObjectError
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

data class ErrorModel(
    var timestamp: ZonedDateTime = now(),
    var path: String? = null,
    var status: HttpStatus,
    var message: String? = null,
    var args: Map<String, Any> = emptyMap(),
    var exceptionType: String? = null,
    var exceptionMessage: String? = null
) {
    companion object {
        private const val DELIMITER: String = "\""
        private const val CONSTRAINT_PREFIX: String = "constraint_"

        private const val TIMESTAMP: String = "timestamp"
        private const val PATH: String = "path"
        private const val STATUS: String = "status"
        private const val ERROR: String = "error"
        private const val MESSAGE: String = "message"
        private const val ARGS: String = "args"
        private const val EXCEPTION_TYPE: String = "exceptionType"
        private const val EXCEPTION_MESSAGE: String = "exceptionMessage"
    }

    constructor(
        exchange: ServerWebExchange,
        exception: Throwable,
        status: HttpStatus,
        message: String,
    ): this(status = status, message = message) {
        path = exchange.request.path.pathWithinApplication().value()
        exceptionType = exception::class.qualifiedName
        exceptionMessage = exception.message

        fillWithException(exception)
    }

    constructor(
        errorAttributes: Map<String, Any>,
        exception: Throwable,
    ): this(status = HttpStatus.valueOf(errorAttributes[STATUS] as Int)) {
        fillWithException(exception)
    }

    private fun fillWithException(exception: Throwable) {
        when (exception) {
            is RegistryExceptionModel -> fillWithRegistryException(exception)
            is WebExchangeBindException -> fillWithFieldError(exception)
            is ServerWebInputException -> fillWithInputException(exception)
            is HandlerMethodValidationException -> fillWithMethodValidationException(exception)
        }

        fillWithGenericException(exception)
    }

    private fun fillWithRegistryException(exception: RegistryExceptionModel) {
        message = exception.message

        if (Objects.nonNull(exception.args)) args = exception.args !!
    }

    private fun fillWithFieldError(exception: WebExchangeBindException) {
        val error: ObjectError = exception.allErrors.first()
        message = error.defaultMessage !!

        if (! error.arguments.isNullOrEmpty() && error.arguments !!.size > 1) {
            error.arguments !!.drop(1).forEachIndexed { index, it ->
                args = args.plus(Pair(CONSTRAINT_PREFIX + index, it.toString()))
            }
        }
    }

    private fun fillWithInputException(exception: ServerWebInputException) {
        if (exception.cause !is TypeMismatchException) return
        val error = exception.cause as TypeMismatchException

        message = PARAMETER_TYPE_MISMATCH

        args = args.plus(Pair("${CONSTRAINT_PREFIX}0", error.propertyName ?: ""))
        args = args.plus(Pair("${CONSTRAINT_PREFIX}1", error.requiredType ?: ""))
        args = args.plus(Pair("${CONSTRAINT_PREFIX}2", error.value ?: ""))
    }

    private fun fillWithMethodValidationException(exception: HandlerMethodValidationException) {
        val error: ParameterValidationResult = exception.valueResults.first()
        message = error.resolvableErrors.first().defaultMessage !!

        args = args.plus(Pair("${CONSTRAINT_PREFIX}0", error.argument ?: ""))
    }

    private fun fillWithGenericException(exception: Throwable) {
        val type = exception.fillInStackTrace()::class.qualifiedName
        if (! type.isNullOrBlank()) exceptionType = type

        val message = exception.fillInStackTrace().localizedMessage?.getStringBetween(DELIMITER)
        if (! message.isNullOrBlank()) exceptionMessage = message
    }

    fun buildError(): MutableMap<String, Any> {
        val errorBody = mutableMapOf<String, Any>(
            TIMESTAMP to Date(),
            STATUS to status.value(),
            ERROR to status.name,
            MESSAGE to (message ?: UNKNOWN_ERROR)
        )
        path?.let { errorBody[PATH] = it }
        args.let { errorBody[ARGS] = it }
        exceptionType?.let { errorBody[EXCEPTION_TYPE] = it }
        exceptionMessage?.let { errorBody[EXCEPTION_MESSAGE] = it }
        return errorBody
    }

    fun buildError(response: ServerHttpResponse): Mono<Void> {
        response.statusCode = status

        val body = response.bufferFactory()
            .wrap(
                GsonConfig().gson().toJson(buildError())
                    .toByteArray()
            )

        return response.writeWith(Mono.just(body))
    }
}
