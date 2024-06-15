package com.laucoin.registry.core.model.util

import java.util.Objects
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class RegistryExceptionModel(
    private val status: HttpStatus,
    val errorCode: String,
    override val cause: Throwable? = null,
    val args: List<Any> = emptyList(),
): ResponseStatusException(status, errorCode, cause) {
    override fun fillInStackTrace(): Throwable {
        return if (Objects.nonNull(cause)) cause !!
        else super.fillInStackTrace()
    }
}
