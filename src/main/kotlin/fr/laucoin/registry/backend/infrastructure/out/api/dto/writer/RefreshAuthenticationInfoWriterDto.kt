package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_BLANK
import jakarta.validation.constraints.NotBlank

data class RefreshAuthenticationInfoWriterDto(
	@field:NotBlank(message = REFRESH_TOKEN_BLANK)
	var refreshToken: String? = null,
)
