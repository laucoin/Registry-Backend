package fr.laucoin.registry.backend.infrastructure.driving.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

@JsonInclude(NON_NULL)
data class ErrorDto(
	var type: String = "about:blank",
	var title: String? = null,
	var status: Int? = null,
	var detail: String? = null,
	var instance: String? = null,
	var code: String? = null,
)
