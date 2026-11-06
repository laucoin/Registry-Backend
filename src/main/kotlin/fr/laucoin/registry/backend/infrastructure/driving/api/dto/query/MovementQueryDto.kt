package fr.laucoin.registry.backend.infrastructure.driving.api.dto.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.web.bind.annotation.BindParam

class MovementQueryDto(
	page: Long = PAGE_DEFAULT.toLong(),
	size: Int = PAGE_SIZE_DEFAULT.toInt(),
	@field:Parameter(description = "\"isCurrent\" means a movement with a REGISTERED participant still outside or a GUEST still inside")
	val isCurrent: Boolean = false,
	@field:Parameter(description = "\"true\" value will be considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
	val hasActivity: Boolean? = null,
	@field:Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
	val isVisible: Boolean? = null,
	val type: MovementTypeEnum? = null,
	@field:Parameter(description = "Start (inclusive) of the period that must contain the element date-time")
	@DateTimeFormat(iso = DATE_TIME)
	val from: ZonedDateTime? = null,
	@field:Parameter(description = "End (inclusive) of the period that must contain the element date-time")
	@DateTimeFormat(iso = DATE_TIME)
	val to: ZonedDateTime? = null,
	@field:Parameter(description = "Only the movements of this activity. Considered only if the project has REGISTRY_PROJECT_OPTION_ACTIVITY.")
	val activityId: UUID? = null,
	@field:Parameter(description = "Sort field, prefixed with - for descending order. Supported: dateTime, type")
	@field:Size(max = SORT_MAX, message = PARAMETER_TOO_LONG)
	val sort: String? = null,
): PageQueryDto(page, size)
