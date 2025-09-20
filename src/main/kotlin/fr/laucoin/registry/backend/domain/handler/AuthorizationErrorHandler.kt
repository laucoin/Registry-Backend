package fr.laucoin.registry.backend.domain.handler

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.constant.ErrorConst.FAILED_TO_LOGIN_FOR_UNKNOWN_REASON
import fr.laucoin.registry.backend.domain.constant.ErrorConst.INVALID_TOKEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.ErrorDto
import java.util.Locale
import org.springframework.context.annotation.Bean
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AuthorizationErrorHandler(
	private val translateService: ITranslateService,
	private val gson: Gson,
): ServerAuthenticationFailureHandler {
	override fun onAuthenticationFailure(
		webFilterExchange: WebFilterExchange,
		exception: AuthenticationException
	): Mono<Void> {
		val response = webFilterExchange.exchange.response
		return when (exception) {
			is JwtConversionException -> response.writeWith(
				buildBody(
					response,
					exception.status,
					exception.code,
					LocaleContextHolder.getLocale()
				)
			)

			is InvalidBearerTokenException -> response.writeWith(
				buildBody(
					response,
					UNAUTHORIZED,
					INVALID_TOKEN,
					LocaleContextHolder.getLocale()
				)
			)

			else -> response.writeWith(
				buildBody(
					response,
					INTERNAL_SERVER_ERROR,
					FAILED_TO_LOGIN_FOR_UNKNOWN_REASON,
					LocaleContextHolder.getLocale()
				)
			)
		}
	}

	@Bean
	fun unauthorizedHandler(): ServerAuthenticationEntryPoint = ServerAuthenticationEntryPoint { exchange, _ ->
		val response = exchange.response
		response.writeWith(buildBody(response, UNAUTHORIZED, NOT_AUTHENTICATED, LocaleContextHolder.getLocale()))
	}

	@Bean
	fun accessDeniedHandler(): ServerAccessDeniedHandler = ServerAccessDeniedHandler { exchange, _ ->
		val response = exchange.response
		response.writeWith(buildBody(response, FORBIDDEN, NOT_ENOUGH_PERMISSION, LocaleContextHolder.getLocale()))
	}

	private fun buildBody(
		response: ServerHttpResponse,
		status: HttpStatus,
		errorCode: String,
		locale: Locale,
	): Mono<DataBuffer> {
		response.statusCode = status
		response.headers.contentType = APPLICATION_JSON

		val error = ErrorDto(
			statusCode = status.value(),
			statusName = status.name,
			code = errorCode,
			title = translateService.getMessage(code = "$ERROR_TITLE_PREFIX${status.value()}", locale = locale),
			message = translateService.getMessage(code = "$ERROR_MESSAGE_PREFIX$errorCode", locale = locale),
		)

		return Mono.just(response.bufferFactory().wrap(gson.toJson(error).toByteArray()))
	}
}
