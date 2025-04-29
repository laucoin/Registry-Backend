package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.ProjectOptionDependencies
import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_BEGIN_LATER_THAN_END_TIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@StartBeforeEnd(startField = "begin", endField = "end", message = PROJECT_BEGIN_LATER_THAN_END_TIME)
data class ProjectWriterDto(
    @field:NotBlank(message = PROJECT_NAME_NULL_OR_BLANK)
    @field:Size(max = 150, message = PROJECT_NAME_TOO_LONG)
    var name: String? = null,
    @field:Valid
    var begin: CustomDateTimeWriterDto? = null,
    @field:Valid
    var end: CustomDateTimeWriterDto? = null,
    @field:ProjectOptionDependencies(message = PROJECT_OPTIONS_MISSING)
    var options: List<ProjectOptionEnum>? = emptyList(),
)
