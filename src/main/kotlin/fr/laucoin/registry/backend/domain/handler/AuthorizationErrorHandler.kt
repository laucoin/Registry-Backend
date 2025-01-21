package fr.laucoin.registry.backend.domain.handler

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ErrorDto
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AuthorizationErrorHandler(private val gson: Gson) {
    @Bean
    fun unauthorizedHandler(): ServerAuthenticationEntryPoint = ServerAuthenticationEntryPoint { exchange, _ ->
        val status = UNAUTHORIZED
        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = APPLICATION_JSON

        val body = ErrorDto(
            statusCode = status.value(),
            statusName = status.name,
            code = NOT_AUTHENTICATED,
            message = NOT_AUTHENTICATED,
        )
        val buffer = response.bufferFactory().wrap(gson.toJson(body).toByteArray())
        response.writeWith(Mono.just(buffer))
    }

    @Bean
    fun accessDeniedHandler(): ServerAccessDeniedHandler = ServerAccessDeniedHandler { exchange, _ ->
        val status = FORBIDDEN
        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = APPLICATION_JSON

        val body = ErrorDto(
            statusCode = status.value(),
            statusName = status.name,
            code = NOT_ENOUGH_PERMISSION,
            message = NOT_ENOUGH_PERMISSION,
        )
        val buffer = response.bufferFactory().wrap(gson.toJson(body).toByteArray())
        response.writeWith(Mono.just(buffer))
    }
}
