package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ErrorDto
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

interface IRegistryControllerAdvice {
    @ExceptionHandler(RegistryException::class)
    fun handleRegistryException(exception: RegistryException): Mono<ResponseEntity<ErrorDto>>

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleWebExchangeBindException(exception: WebExchangeBindException): Mono<ResponseEntity<ErrorDto>>

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(exception: ServerWebInputException): Mono<ResponseEntity<ErrorDto>>

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(exception: HandlerMethodValidationException): Mono<ResponseEntity<ErrorDto>>

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleHandlerAuthorizationDeniedException(exception: AuthorizationDeniedException): Mono<ResponseEntity<ErrorDto>>

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): Mono<ResponseEntity<ErrorDto>>

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): Mono<ResponseEntity<ErrorDto>>
}
