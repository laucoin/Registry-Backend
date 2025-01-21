package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.EventOptionDependencies
import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_BEGIN_LATER_THAN_END_TIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_NAME_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime

@StartBeforeEnd(startField = "begin", endField = "end", message = EVENT_BEGIN_LATER_THAN_END_TIME)
data class EventWriterDto(
    @field:NotBlank(message = EVENT_NAME_BLANK)
    @field:Size(max = 150, message = EVENT_NAME_TOO_LONG)
    var name: String? = null,
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
    @field:EventOptionDependencies(message = EVENT_OPTIONS_MISSING)
    var options: List<EventOptionEnum>? = emptyList(),
)
