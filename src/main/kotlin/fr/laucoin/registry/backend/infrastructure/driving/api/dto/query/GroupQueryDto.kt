package fr.laucoin.registry.backend.infrastructure.driving.api.dto.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.web.bind.annotation.BindParam

class GroupQueryDto(
	page: Long = PAGE_DEFAULT.toLong(),
	size: Int = PAGE_SIZE_DEFAULT.toInt(),
	@field:Parameter(description = "Text matched against the group name (accent-insensitive substring)")
	@field:Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG)
	@param:BindParam("q")
	val query: String? = null,
	@field:Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
	val isVisible: Boolean? = null,
	@field:Parameter(description = "Group availability filter at the current time: true keeps groups currently in their availability window, false keeps the others, omitted keeps both")
	val isPresent: Boolean? = null,
	@field:Parameter(description = "Instant that must fall inside the validity window")
	@DateTimeFormat(iso = DATE_TIME)
	val at: ZonedDateTime? = null,
	@field:Parameter(description = "Membership filter: true keeps the groups having a member linked to the signed-in user, omitted or false keeps every group")
	val mine: Boolean? = null,
	@field:Parameter(description = "On-site members filter: true keeps the groups having a member that arrived and has not left yet, false keeps the others, omitted keeps both")
	val hasOnSiteMembers: Boolean? = null,
	@field:Parameter(description = "Sort field, prefixed with - for descending order. Supported: name, startAvailability, endAvailability")
	@field:Size(max = SORT_MAX, message = PARAMETER_TOO_LONG)
	val sort: String? = null,
): PageQueryDto(page, size)
