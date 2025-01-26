package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

@JsonInclude(NON_NULL)
data class ErrorDto(
    var statusCode: Int? = null,
    var statusName: String? = null,
    var code: String? = null,
    var title: String? = null,
    var message: String? = null,
)

