package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_START_LATER_THAN_END
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID

@StartBeforeEnd(startField = "begin", endField = "end", message = GROUP_START_LATER_THAN_END)
data class GroupWriterDto(
    @field:NotBlank(message = GROUP_NAME_NULL_OR_BLANK)
    @field:Size(max = 150, message = GROUP_NAME_TOO_LONG)
    var name: String? = null,
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
    @field:Valid
    @field:NotEmpty(message = GROUP_MEMBERS_EMPTY)
    var members: List<UUID>? = null,
)
