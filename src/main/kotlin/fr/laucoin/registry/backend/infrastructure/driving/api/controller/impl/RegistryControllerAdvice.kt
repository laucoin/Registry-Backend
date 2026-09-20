package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UNKNOWN_ERROR
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IRegistryControllerAdvice
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.ErrorDto
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
		val errors = exception.allErrors.map { error ->
			error.defaultMessage!! to (error.arguments?.drop(1)?.toTypedArray() ?: emptyArray())
		}
		return buildError(exchange, status = BAD_REQUEST, errors = errors)
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
		val errors = exception.valueResults.flatMap { result: ParameterValidationResult ->
			result.resolvableErrors.map { resolvable ->
				resolvable.defaultMessage!! to (resolvable.arguments?.drop(1)?.toTypedArray() ?: emptyArray())
			}
		}
		return buildError(exchange, status = BAD_REQUEST, errors = errors)
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
		exception: Exception? = null,
	): Mono<ResponseEntity<ErrorDto>> = buildError(exchange, status, listOf(code to (args ?: emptyArray())), exception)

	/**
	 * [errors] holds one (code, args) pair per failed constraint. A single
	 * entry keeps today's flat wire shape (`errors` stays absent); more than
	 * one populates `errors` with every translated failure instead of
	 * silently dropping all but the first, while the top-level `code`/
	 * `title`/`message` still carry the first one for clients that only read
	 * those.
	 */
	private fun buildError(
		exchange: ServerWebExchange,
		status: HttpStatus,
		errors: List<Pair<String, Array<Any>>>,
		exception: Exception? = null,
	): Mono<ResponseEntity<ErrorDto>> {
		errors.forEach { (code, args) -> logError(status, code, args, exception) }

		val locale = localeContextResolver.resolveLocaleContext(exchange).locale ?: Locale.getDefault()
		val translated = errors.map { (code, args) -> translateOne(status, code, args, locale) }
		val body = if (translated.size > 1) translated.first().copy(errors = translated) else translated.first()
		return Mono.just(ResponseEntity.status(status).body(body))
	}

	private fun logError(status: HttpStatus, code: String, args: Array<Any>, exception: Exception?) {
		if (status.is5xxServerError) {
			log.error("An error (${status.value()}) occurred with code $code", *args, exception)
		} else {
			log.info("Return a status ${status.value()} with code $code", *args)
		}
	}

	private fun translateOne(status: HttpStatus, code: String, args: Array<Any>, locale: Locale): ErrorDto {
		val title = translateService.getError(code = "$ERROR_TITLE_PREFIX${status.value()}", locale = locale)
		val message = translateService.getError(
			code = "$ERROR_MESSAGE_PREFIX$code",
			args = args,
			default = translateService.getError(code = "$ERROR_MESSAGE_PREFIX$UNKNOWN_ERROR", locale = locale),
			locale = locale,
		)
		return ErrorDto(status.value(), status.name, code, title, message)
	}
}
