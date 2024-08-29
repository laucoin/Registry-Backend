package fr.laucoin.registry.backend.domain.model

import java.util.Objects
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

data class RegistryExceptionModel(
    val status: HttpStatus,
    override val message: String,
    override val cause: Throwable? = null,
    val args: Map<String, String>? = null,
): ResponseStatusException(status, message, cause) {

    override fun fillInStackTrace(): Throwable {
        return if (Objects.nonNull(cause)) cause !!
        else super.fillInStackTrace()
    }
}
