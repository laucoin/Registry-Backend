package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.STATE_BLANK
import jakarta.validation.constraints.NotBlank

data class AuthenticationInfoModel(
	@field:NotBlank(message = REDIRECT_URI_BLANK)
	val redirectUri: String?,
	@field:NotBlank(message = AUTHORIZATION_CODE_BLANK)
	val authorizationCode: String?,
	/** Echoed back from the callback, to be matched against the value held in the challenge cookie. */
	@field:NotBlank(message = STATE_BLANK)
	val state: String? = null,
)
