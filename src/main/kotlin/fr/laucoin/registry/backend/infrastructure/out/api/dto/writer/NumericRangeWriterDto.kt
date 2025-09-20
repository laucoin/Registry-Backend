package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.annotation.MinUpperMax
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_MAX_IS_HIGHER_THAN_MIN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@MinUpperMax(startField = "lower", endField = "upper", message = ACTIVITY_ALLOWED_PARTICIPANTS_MAX_IS_HIGHER_THAN_MIN)
data class NumericRangeWriterDto(
	@field:NotNull(message = ACTIVITY_ALLOWED_PARTICIPANTS_NULL)
	@field:Min(1, message = ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW)
	var lower: Int? = null,
	@field:NotNull(message = ACTIVITY_ALLOWED_PARTICIPANTS_NULL)
	var upper: Int? = null,
)
