package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import jakarta.validation.constraints.NotBlank

data class AuthenticationInfoWriterDto(
	@field:NotBlank(message = REDIRECT_URI_BLANK)
	var redirectUri: String? = null,
	@field:NotBlank(message = AUTHORIZATION_CODE_BLANK)
	var authorizationCode: String? = null,
)
