package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.LIST_TOO_LARGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_BEGIN_LATER_THAN_END_TIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PROJECT_NAME_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PROJECT_OPTIONS_MAX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.ProjectOptionDependencies
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.StartBeforeEnd
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@StartBeforeEnd(startField = "begin", endField = "end", message = PROJECT_BEGIN_LATER_THAN_END_TIME)
data class ProjectWriterDto(
	@field:NotBlank(message = PROJECT_NAME_NULL_OR_BLANK)
	@field:Size(max = PROJECT_NAME_MAX, message = PROJECT_NAME_TOO_LONG)
	var name: String? = null,
	@field:Valid
	var begin: CustomDateTimeWriterDto? = null,
	@field:Valid
	var end: CustomDateTimeWriterDto? = null,
	@field:ProjectOptionDependencies(message = PROJECT_OPTIONS_MISSING)
	@field:Size(max = PROJECT_OPTIONS_MAX, message = LIST_TOO_LARGE)
	var options: List<ProjectOptionEnum>? = emptyList(),
)
