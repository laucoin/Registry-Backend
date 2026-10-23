package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DESCRIPTION_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ACTIVITY_DESCRIPTION_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ACTIVITY_DURATION_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ACTIVITY_NAME_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.DURATION_PATTERN
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.StartBeforeEnd
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@StartBeforeEnd(startField = "startAvailability", endField = "endAvailability", message = ACTIVITY_START_LATER_THAN_END)
data class ActivityWriterDto(
	@field:NotBlank(message = ACTIVITY_NAME_NULL_OR_BLANK)
	@field:Size(max = ACTIVITY_NAME_MAX, message = ACTIVITY_NAME_TOO_LONG)
	var name: String? = null,
	@field:Size(max = ACTIVITY_DESCRIPTION_MAX, message = ACTIVITY_DESCRIPTION_TOO_LONG)
	var description: String? = null,
	@field:Size(max = ACTIVITY_DURATION_MAX, message = PARAMETER_TOO_LONG)
	@field:Pattern(regexp = DURATION_PATTERN, message = PARAMETER_FORMAT_INVALID)
	var duration: String? = null,
	@field:Valid
	var allowedParticipants: NumericRangeWriterDto? = null,
	@field:Valid
	var startAvailability: CustomDateTimeWriterDto? = null,
	@field:Valid
	var endAvailability: CustomDateTimeWriterDto? = null,
)
