package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_BLANK
import jakarta.validation.constraints.NotBlank

data class RefreshAuthenticationInfoModel(
	@field:NotBlank(message = REFRESH_TOKEN_BLANK)
	val refreshToken: String?,
)
