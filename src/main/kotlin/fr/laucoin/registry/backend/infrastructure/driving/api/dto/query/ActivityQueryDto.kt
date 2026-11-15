package fr.laucoin.registry.backend.infrastructure.driving.api.dto.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ACTIVITY_DURATION_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.DURATION_PATTERN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.web.bind.annotation.BindParam

class ActivityQueryDto(
	page: Long = PAGE_DEFAULT.toLong(),
	size: Int = PAGE_SIZE_DEFAULT.toInt(),
	@field:Parameter(description = "Text matched against the activity name and description")
	@field:Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG)
	@param:BindParam("q")
	val query: String? = null,
	@field:Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
	val isVisible: Boolean? = null,
	@field:Parameter(description = "Availability filter at the current time: true keeps available elements only, false keeps unavailable ones only, omitted keeps both")
	val isAvailable: Boolean? = null,
	@field:Parameter(description = "Instant that must fall inside the validity window")
	@DateTimeFormat(iso = DATE_TIME)
	val at: ZonedDateTime? = null,
	@field:Parameter(description = "Minimum activity duration (inclusive), ISO 8601 such as PT1H30M. Activities without duration are excluded")
	@field:Size(max = ACTIVITY_DURATION_MAX, message = PARAMETER_TOO_LONG)
	@field:Pattern(regexp = DURATION_PATTERN, message = PARAMETER_FORMAT_INVALID)
	val minDuration: String? = null,
	@field:Parameter(description = "Maximum activity duration (inclusive), ISO 8601 such as PT1H30M. Activities without duration are excluded")
	@field:Size(max = ACTIVITY_DURATION_MAX, message = PARAMETER_TOO_LONG)
	@field:Pattern(regexp = DURATION_PATTERN, message = PARAMETER_FORMAT_INVALID)
	val maxDuration: String? = null,
	@field:Parameter(description = "Number of participants the activity must be able to host, within its allowed participants range")
	@field:Min(1, message = ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW)
	val participants: Int? = null,
	@field:Parameter(description = "Sort field, prefixed with - for descending order. Supported: name, description, duration, minAllowedParticipants, maxAllowedParticipants, startAvailability, endAvailability")
	@field:Size(max = SORT_MAX, message = PARAMETER_TOO_LONG)
	val sort: String? = null,
): PageQueryDto(page, size)
