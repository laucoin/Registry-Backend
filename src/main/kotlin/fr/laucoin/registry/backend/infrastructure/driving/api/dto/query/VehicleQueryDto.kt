package fr.laucoin.registry.backend.infrastructure.driving.api.dto.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.web.bind.annotation.BindParam

class VehicleQueryDto(
	page: Long = PAGE_DEFAULT.toLong(),
	size: Int = PAGE_SIZE_DEFAULT.toInt(),
	@field:Parameter(description = "Text matched against the vehicle license plate, brand and model")
	@field:Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG)
	@param:BindParam("q")
	val query: String? = null,
	@field:Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
	val isVisible: Boolean? = null,
	val status: PresenceStatusEnum? = null,
	@field:Parameter(description = "Instant that must fall inside the validity window")
	@DateTimeFormat(iso = DATE_TIME)
	val at: ZonedDateTime? = null,
	@field:Parameter(description = "Exact vehicle brand, ignoring case and accents")
	@field:Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG)
	val brand: String? = null,
	@field:Parameter(description = "Exact vehicle model, ignoring case and accents")
	@field:Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG)
	val model: String? = null,
	@field:Parameter(description = "Sort field, prefixed with - for descending order. Supported: licensePlate, brand, model, startAvailability, endAvailability")
	@field:Size(max = SORT_MAX, message = PARAMETER_TOO_LONG)
	val sort: String? = null,
): PageQueryDto(page, size)
