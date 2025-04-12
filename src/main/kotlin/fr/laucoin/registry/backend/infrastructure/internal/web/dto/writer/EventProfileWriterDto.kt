package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@StartBeforeEnd(startField = "startAccess", endField = "endAccess", message = EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS)
data class EventProfileWriterDto(
    @field:NotBlank(message = EVENT_PROFILE_ROLE_BLANK)
    var role: String? = null,
    @field:Valid
    var startAccess: CustomDateTimeWriterDto? = null,
    @field:Valid
    var endAccess: CustomDateTimeWriterDto? = null,
)
