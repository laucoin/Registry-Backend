package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ErrorDto
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ServerWebInputException

interface IRegistryControllerAdvice {
    @ExceptionHandler(RegistryException::class)
    fun handleRegistryException(exception: RegistryException): ResponseEntity<ErrorDto>

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleWebExchangeBindException(exception: WebExchangeBindException): ResponseEntity<ErrorDto>

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(exception: ServerWebInputException): ResponseEntity<ErrorDto>

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(exception: HandlerMethodValidationException): ResponseEntity<ErrorDto>

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleHandlerAuthorizationDeniedException(exception: AuthorizationDeniedException): ResponseEntity<ErrorDto>

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ErrorDto>
}
