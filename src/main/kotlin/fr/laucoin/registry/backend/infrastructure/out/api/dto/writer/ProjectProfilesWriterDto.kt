package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_USERS_EMPTY
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

@StartBeforeEnd(
	startField = "startAccess",
	endField = "endAccess",
	message = PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
)
data class ProjectProfilesWriterDto(
	@field:NotEmpty(message = PROJECT_PROFILE_USERS_EMPTY)
	var userIds: List<UUID>? = null,
	@field:NotBlank(message = PROJECT_PROFILE_ROLE_BLANK)
	var role: String? = null,
	@field:Valid
	var startAccess: CustomDateTimeWriterDto? = null,
	@field:Valid
	var endAccess: CustomDateTimeWriterDto? = null,
)
