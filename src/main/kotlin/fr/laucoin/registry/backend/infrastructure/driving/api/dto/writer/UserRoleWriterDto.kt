package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ROLE_PATTERN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.USER_ROLE_MAX
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserRoleWriterDto(
	@field:Size(max = USER_ROLE_MAX, message = PARAMETER_TOO_LONG)
	@field:Pattern(regexp = ROLE_PATTERN, message = PARAMETER_FORMAT_INVALID)
	var role: String? = null,
)
