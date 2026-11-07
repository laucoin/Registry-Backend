package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.AUTHENTICATION_AUTHORIZATION_CODE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.AUTHENTICATION_REDIRECT_URI_MAX
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AuthenticationInfoWriterDto(
	@field:NotBlank(message = REDIRECT_URI_BLANK)
	@field:Size(max = AUTHENTICATION_REDIRECT_URI_MAX, message = PARAMETER_TOO_LONG)
	val redirectUri: String?,
	@field:NotBlank(message = AUTHORIZATION_CODE_BLANK)
	@field:Size(max = AUTHENTICATION_AUTHORIZATION_CODE_MAX, message = PARAMETER_TOO_LONG)
	val authorizationCode: String?,
)
