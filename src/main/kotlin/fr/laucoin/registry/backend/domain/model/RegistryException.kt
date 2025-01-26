package fr.laucoin.registry.backend.domain.model

import org.springframework.http.HttpStatus

data class RegistryException(
    val status: HttpStatus,
    val code: String,
    val args: ArrayList<Any?>? = null,
): Exception(code)
