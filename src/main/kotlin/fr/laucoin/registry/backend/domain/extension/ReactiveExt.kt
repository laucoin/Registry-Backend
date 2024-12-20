package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.WRONG_AUTHENTICATION_MOD
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryExceptionModel
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

object ReactiveExt {
    fun <T> Mono<T>.notFoundIfEmpty(identifier: Any): Mono<T> {
        return switchIfEmpty {
            LoggerFactory.getLogger(this::class.java).warn(
                "Not data found with the given identifier ({}) and the current user permissions",
                identifier
            )
            throw RegistryExceptionModel(
                status = NOT_FOUND,
                message = NOT_FOUND_WITH_GIVEN_IDENTIFIER,
                args = mapOf(Pair("identifier", identifier.toString())),
            )
        }
    }

    fun currentUser(): Mono<CurrentUserModel> {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .cast(Authentication::class.java)
            .map { it.principal as CurrentUserModel }
            .onErrorMap {
                val exception = RegistryExceptionModel(UNPROCESSABLE_ENTITY, WRONG_AUTHENTICATION_MOD, cause = it)
                LoggerFactory.getLogger(this::class.java).error("Failed to map current user.", exception)
                throw exception
            }
    }
}
