package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.domain.constant.ErrorConst.FAILED_TO_LOGIN_FOR_UNKNOWN_REASON
import fr.laucoin.registry.backend.domain.constant.ErrorConst.INVALID_TOKEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.ErrorDto
import java.util.Locale
import org.springframework.context.annotation.Bean
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper

/**
 * Purpose: Writes the problem responses of authentication and authorization failures.
 * Scope: Answers 401, 403 and token conversion errors with a localized body.
 * Limits: Never exposes the failure cause.
 */
@Component
class AuthorizationErrorHandler(
	private val translateService: ITranslateService,
	private val localeContextResolver: LocaleContextResolver,
	private val objectMapper: ObjectMapper,
): ServerAuthenticationFailureHandler {
	override fun onAuthenticationFailure(
		webFilterExchange: WebFilterExchange,
		exception: AuthenticationException
	): Mono<Void> {
		val exchange = webFilterExchange.exchange
		val response = exchange.response
		return response.writeWith(
			when (exception) {
				is JwtConversionException ->
					buildBody(exchange, response, exception.status, exception.code, exception.args?.toArray())

				is InvalidBearerTokenException -> buildBody(exchange, response, UNAUTHORIZED, INVALID_TOKEN)

				else -> buildBody(exchange, response, INTERNAL_SERVER_ERROR, FAILED_TO_LOGIN_FOR_UNKNOWN_REASON)
			}
		)
	}

	@Bean
	fun unauthorizedHandler(): ServerAuthenticationEntryPoint = ServerAuthenticationEntryPoint { exchange, _ ->
		val response = exchange.response
		response.writeWith(buildBody(exchange, response, UNAUTHORIZED, NOT_AUTHENTICATED))
	}

	@Bean
	fun accessDeniedHandler(): ServerAccessDeniedHandler = ServerAccessDeniedHandler { exchange, _ ->
		val response = exchange.response
		response.writeWith(buildBody(exchange, response, FORBIDDEN, NOT_ENOUGH_PERMISSION))
	}

	private fun buildBody(
		exchange: ServerWebExchange,
		response: ServerHttpResponse,
		status: HttpStatus,
		errorCode: String,
		args: Array<Any>? = null,
	): Mono<DataBuffer> {
		response.statusCode = status
		response.headers.contentType = APPLICATION_PROBLEM_JSON

		val locale = localeContextResolver.resolveLocaleContext(exchange).locale ?: Locale.getDefault()
		val error = ErrorDto(
			title = translateService.getError(code = "$ERROR_TITLE_PREFIX${status.value()}", locale = locale),
			status = status.value(),
			detail = translateService.getError(code = "$ERROR_MESSAGE_PREFIX$errorCode", args = args, locale = locale),
			instance = exchange.request.path.value(),
			code = errorCode,
		)

		return Mono.just(response.bufferFactory().wrap(objectMapper.writeValueAsBytes(error)))
	}
}
