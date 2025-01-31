package fr.laucoin.registry.backend.domain.handler

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.constant.ErrorConst.FAILED_TO_LOGIN_FOR_UNKNOWN_REASON
import fr.laucoin.registry.backend.domain.constant.ErrorConst.INVALID_TOKEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.handler.HeadersHandler.Companion.getLocaleOrThrow
import fr.laucoin.registry.backend.domain.handler.HeadersHandler.Companion.headers
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ErrorDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.server.reactive.ServerHttpRequest
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
    @Value("\${registry.information.locale.supported}")
    private val supportedLocales: List<String>,
    @Qualifier("errorsSource")
    private val translateService: MessageSource,
    private val gson: Gson,
): ServerAuthenticationFailureHandler {
    override fun onAuthenticationFailure(webFilterExchange: WebFilterExchange, exception: AuthenticationException): Mono<Void> {
        val response = webFilterExchange.exchange.response
        val locale = extractLocale(webFilterExchange.exchange.request)
        return when (exception) {
            is JwtConversionException -> response.writeWith(buildBody(response, exception.status, exception.code, locale))
            is InvalidBearerTokenException -> response.writeWith(buildBody(response, UNAUTHORIZED, INVALID_TOKEN, locale))
            else -> response.writeWith(buildBody(response, INTERNAL_SERVER_ERROR, FAILED_TO_LOGIN_FOR_UNKNOWN_REASON, locale))
        }
    }

    @Bean
    fun unauthorizedHandler(): ServerAuthenticationEntryPoint = ServerAuthenticationEntryPoint { exchange, _ ->
        val response = exchange.response
        response.writeWith(buildBody(response, UNAUTHORIZED, NOT_AUTHENTICATED, extractLocale(exchange.request)))
    }

    @Bean
    fun accessDeniedHandler(): ServerAccessDeniedHandler = ServerAccessDeniedHandler { exchange, _ ->
        val response = exchange.response
        response.writeWith(buildBody(response, FORBIDDEN, NOT_ENOUGH_PERMISSION, extractLocale(exchange.request)))
    }

    private fun extractLocale(request: ServerHttpRequest): Locale {
        val headers: Map<String, String> = headers(request)
        return try {
            getLocaleOrThrow(headers, supportedLocales)
        } catch (e: RegistryException) {
            Locale.getDefault()
        }
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
            title = translateService.getMessage("$ERROR_TITLE_PREFIX${status.value()}", null, locale),
            message = translateService.getMessage("$ERROR_MESSAGE_PREFIX$errorCode", null, locale),
        )

        return Mono.just(response.bufferFactory().wrap(gson.toJson(error).toByteArray()))
    }
}
