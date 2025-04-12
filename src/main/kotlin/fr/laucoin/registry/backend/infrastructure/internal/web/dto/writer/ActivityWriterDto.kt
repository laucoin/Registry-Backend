package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DESCRIPTION_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_START_LATER_THAN_END
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@StartBeforeEnd(startField = "startAvailability", endField = "endAvailability", message = ACTIVITY_START_LATER_THAN_END)
data class ActivityWriterDto(
    @field:NotBlank(message = ACTIVITY_NAME_NULL_OR_BLANK)
    @field:Size(max = 150, message = ACTIVITY_NAME_TOO_LONG)
    var name: String? = null,
    @field:Size(max = 2000, message = ACTIVITY_DESCRIPTION_TOO_LONG)
    var description: String? = null,
    var duration: String? = null,
    @field:Valid
    var allowedParticipants: NumericRangeWriterDto? = null,
    @field:Valid
    var startAvailability: CustomDateTimeWriterDto? = null,
    @field:Valid
    var endAvailability: CustomDateTimeWriterDto? = null,
)
