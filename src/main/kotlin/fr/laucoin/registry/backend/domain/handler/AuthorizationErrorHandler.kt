package fr.laucoin.registry.backend.domain.handler

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ErrorDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AuthorizationErrorHandler(
    @Qualifier("errorsSource") private val translateService: MessageSource,
    private val gson: Gson,
) {
    @Bean
    fun unauthorizedHandler(): ServerAuthenticationEntryPoint = ServerAuthenticationEntryPoint { exchange, _ ->
        val response = exchange.response
        response.writeWith(buildBody(response, UNAUTHORIZED, NOT_AUTHENTICATED))
    }

    @Bean
    fun accessDeniedHandler(): ServerAccessDeniedHandler = ServerAccessDeniedHandler { exchange, _ ->
        val response = exchange.response
        response.writeWith(buildBody(response, FORBIDDEN, NOT_ENOUGH_PERMISSION))
    }

    private fun buildBody(response: ServerHttpResponse, status: HttpStatus, errorCode: String): Mono<DataBuffer> {
        response.statusCode = status
        response.headers.contentType = APPLICATION_JSON

        val error = ErrorDto(
            statusCode = status.value(),
            statusName = status.name,
            code = errorCode,
            message = translateService.getMessage("$ERROR_MESSAGE_PREFIX$errorCode", null, Locale.getDefault()),
        )

        return Mono.just(response.bufferFactory().wrap(gson.toJson(error).toByteArray()))
    }
}
