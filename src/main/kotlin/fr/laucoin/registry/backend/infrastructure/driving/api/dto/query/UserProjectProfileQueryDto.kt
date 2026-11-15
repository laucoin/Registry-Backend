package fr.laucoin.registry.backend.infrastructure.driving.api.dto.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PROJECT_PROFILE_ROLE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ROLE_PATTERN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.web.bind.annotation.BindParam

class UserProjectProfileQueryDto(
	page: Long = PAGE_DEFAULT.toLong(),
	size: Int = PAGE_SIZE_DEFAULT.toInt(),
	@field:Parameter(description = "Text matched against the project name (accent-insensitive substring)")
	@field:Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG)
	@param:BindParam("q")
	val query: String? = null,
	@field:Parameter(description = "Availability filter at the current time: true keeps available elements only, false keeps unavailable ones only, omitted keeps both")
	val isAvailable: Boolean? = null,
	val status: ProfileStatusEnum? = null,
	@field:Parameter(description = "Instant that must fall inside the validity window")
	@DateTimeFormat(iso = DATE_TIME)
	val at: ZonedDateTime? = null,
	@field:Parameter(description = "Exact role of the profile, as listed by the roles endpoint")
	@field:Size(max = PROJECT_PROFILE_ROLE_MAX, message = PARAMETER_TOO_LONG)
	@field:Pattern(regexp = ROLE_PATTERN, message = PARAMETER_FORMAT_INVALID)
	val role: String? = null,
	@field:Parameter(description = "Sort field, prefixed with - for descending order. Supported: projectName, role, status, startAccess, endAccess")
	@field:Size(max = SORT_MAX, message = PARAMETER_TOO_LONG)
	val sort: String? = null,
): PageQueryDto(page, size)
