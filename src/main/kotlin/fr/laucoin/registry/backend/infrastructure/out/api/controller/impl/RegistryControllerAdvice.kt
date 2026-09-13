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
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono

@RestControllerAdvice
class RegistryControllerAdvice(
	private val translateService: ITranslateService,
	private val localeContextResolver: LocaleContextResolver,
) : IRegistryControllerAdvice, LoggerService() {
	override fun handleRegistryException(
		exception: RegistryException,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ErrorDto>> {
		return buildError(exchange, exception.status, exception.code, exception.args?.toArray())
	}

	override fun handleWebExchangeBindException(
		exception: WebExchangeBindException,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ErrorDto>> {
		val error = exception.allErrors.first()
		return buildError(
			exchange,
			status = BAD_REQUEST,
			code = error.defaultMessage!!,
			args = error.arguments?.drop(1)?.toTypedArray() ?: emptyArray(),
		)
	}

	override fun handleServerWebInputException(
		exception: ServerWebInputException,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ErrorDto>> {
		return buildError(
			exchange,
			status = BAD_REQUEST,
			code = PARAMETER_TYPE_MISMATCH,
			args = arrayOf(exception.cause).filterNotNull().toTypedArray(),
		)
	}

	override fun handleHandlerMethodValidationException(
		exception: HandlerMethodValidationException,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ErrorDto>> {
		val error: ParameterValidationResult = exception.valueResults.first()
		val resolvable = error.resolvableErrors.first()
		return buildError(
			exchange,
			status = BAD_REQUEST,
			code = resolvable.defaultMessage!!,
			args = resolvable.arguments?.drop(1)?.toTypedArray() ?: emptyArray(),
		)
	}

	override fun handleHandlerAuthorizationDeniedException(
		exception: AuthorizationDeniedException,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ErrorDto>> {
		return buildError(exchange, status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
	}

	override fun handleResponseStatusException(
		exception: ResponseStatusException,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ErrorDto>> {
		return buildError(
			exchange,
			status = HttpStatus.valueOf(exception.statusCode.value()),
			code = exception.statusCode.value().toString(),
		)
	}

	override fun handleException(exception: Exception, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorDto>> {
		return buildError(exchange, status = INTERNAL_SERVER_ERROR, code = "500", exception = exception)
	}

	private fun buildError(
		exchange: ServerWebExchange,
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

		val locale = localeContextResolver.resolveLocaleContext(exchange).locale ?: Locale.getDefault()
		val title = translateService.getError(code = "$ERROR_TITLE_PREFIX${status.value()}", locale = locale)
		val message = translateService.getError(
			code = "$ERROR_MESSAGE_PREFIX$code",
			args = args,
			default = translateService.getError(code = "$ERROR_MESSAGE_PREFIX$UNKNOWN_ERROR", locale = locale),
			locale = locale,
		)

		val body = ErrorDto(status.value(), status.name, code, title, message)
		return Mono.just(ResponseEntity.status(status).body(body))
	}
}
