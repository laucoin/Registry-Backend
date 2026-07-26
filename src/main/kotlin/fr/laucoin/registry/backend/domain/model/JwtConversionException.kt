package fr.laucoin.registry.backend.domain.model

import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException

data class JwtConversionException(
	val status: HttpStatus,
	val code: String,
	val args: ArrayList<Any?>? = null,
) : AuthenticationException(code)
