package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.model.ErrorModel
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class ErrorHandler: DefaultErrorAttributes() {
    override fun getErrorAttributes(request: ServerRequest, options: ErrorAttributeOptions): MutableMap<String, Any> {
        return ErrorModel(
            super.getErrorAttributes(request, options),
            getError(request),
        ).buildError()
    }

    @Bean
    fun unauthorizedHandler(): ServerAuthenticationEntryPoint = ServerAuthenticationEntryPoint { exchange, exception ->
        ErrorModel(exchange, exception, UNAUTHORIZED, NOT_AUTHENTICATED)
            .buildError(exchange.response)
    }

    @Bean
    fun accessDeniedHandler(): ServerAccessDeniedHandler = ServerAccessDeniedHandler { exchange, exception ->
        ErrorModel(exchange, exception, FORBIDDEN, NOT_ENOUGH_PERMISSION)
            .buildError(exchange.response)
    }
}
