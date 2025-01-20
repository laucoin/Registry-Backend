package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_USERS_EMPTY
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.ZonedDateTime
import java.util.UUID

@StartBeforeEnd(startField = "startAccess", endField = "endAccess", message = EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS)
data class EventProfilesWriterDto(
    @field:NotEmpty(message = EVENT_PROFILE_USERS_EMPTY)
    var userIds: List<UUID>? = null,
    @field:NotBlank(message = EVENT_PROFILE_ROLE_BLANK)
    var role: String? = null,
    var startAccess: ZonedDateTime? = null,
    var endAccess: ZonedDateTime? = null,
)
