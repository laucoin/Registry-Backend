package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PROJECT_PROFILE_ROLE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ROLE_PATTERN
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.StartBeforeEnd
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@StartBeforeEnd(
	startField = "startAccess",
	endField = "endAccess",
	message = PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
)
data class ProjectProfileWriterDto(
	@field:NotBlank(message = PROJECT_PROFILE_ROLE_BLANK)
	@field:Size(max = PROJECT_PROFILE_ROLE_MAX, message = PARAMETER_TOO_LONG)
	@field:Pattern(regexp = ROLE_PATTERN, message = PARAMETER_FORMAT_INVALID)
	var role: String? = null,
	@field:Valid
	var startAccess: CustomDateTimeWriterDto? = null,
	@field:Valid
	var endAccess: CustomDateTimeWriterDto? = null,
)
