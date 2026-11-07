package fr.laucoin.registry.backend.infrastructure.driving.api.dto.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.web.bind.annotation.BindParam

class ActivityMovementQueryDto(
	page: Long = PAGE_DEFAULT.toLong(),
	size: Int = PAGE_SIZE_DEFAULT.toInt(),
	@field:Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
	val isVisible: Boolean? = null,
	val type: MovementTypeEnum? = null,
	@field:Parameter(description = "Start (inclusive) of the period that must contain the element date-time")
	@DateTimeFormat(iso = DATE_TIME)
	val from: ZonedDateTime? = null,
	@field:Parameter(description = "End (inclusive) of the period that must contain the element date-time")
	@DateTimeFormat(iso = DATE_TIME)
	val to: ZonedDateTime? = null,
	@field:Parameter(description = "Sort field, prefixed with - for descending order. Supported: dateTime, type")
	@field:Size(max = SORT_MAX, message = PARAMETER_TOO_LONG)
	val sort: String? = null,
): PageQueryDto(page, size)
