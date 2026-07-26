package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UNKNOWN_ERROR
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IRegistryControllerAdvice
import fr.laucoin.registry.backend.infrastructure.out.api.dto.ErrorDto
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
		return buildError(status = BAD_REQUEST, code = error.defaultMessage!!, args = error.arguments ?: emptyArray())
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
		return buildError(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
	}

	override fun handleResponseStatusException(exception: ResponseStatusException): Mono<ResponseEntity<ErrorDto>> {
		return buildError(
			status = HttpStatus.valueOf(exception.statusCode.value()),
			code = exception.statusCode.value().toString(),
		)
	}

	override fun handleException(exception: Exception): Mono<ResponseEntity<ErrorDto>> {
		return buildError(status = INTERNAL_SERVER_ERROR, code = "500", exception = exception)
	}

	/**
	 * error.title.* / error.message.* live in the errors bundle (errorsSource),
	 * not the messages bundle — resolving them through getMessage() returned
	 * the raw keys to clients.
	 */
	private fun buildError(
		status: HttpStatus,
		code: String,
		args: Array<Any>? = null,
		exception: Exception? = null
	): Mono<ResponseEntity<ErrorDto>> {
		if (status.is5xxServerError) {
			log.error("An error (${status.value()}) occurred with code $code", *args.orEmpty(), exception)
		} else {
			log.info("Return a status ${status.value()} with code $code", *args.orEmpty())
		}

		val title = translateService.getError(code = "$ERROR_TITLE_PREFIX${status.value()}")
		val message = translateService.getError(
			code = "$ERROR_MESSAGE_PREFIX$code",
			args = args,
			default = translateService.getError(code = "$ERROR_MESSAGE_PREFIX$UNKNOWN_ERROR"),
		)

		val body = ErrorDto(status.value(), status.name, code, title, message)
		return Mono.just(ResponseEntity.status(status).body(body))
	}
}
