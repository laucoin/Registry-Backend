package fr.laucoin.registry.backend.infrastructure.driving.api.dto.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_DEFAULT
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SORT_MAX
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.ZonedDateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import org.springframework.web.bind.annotation.BindParam

class ParticipantQueryDto(
	page: Long = PAGE_DEFAULT.toLong(),
	size: Int = PAGE_SIZE_DEFAULT.toInt(),
	@field:Parameter(description = "Text matched against the participant first name and last name")
	@field:Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG)
	@param:BindParam("q")
	val query: String? = null,
	@field:Parameter(description = "Age filter: true keeps adults (18 or older) only, false keeps minors only, omitted keeps both")
	val isMajor: Boolean? = null,
	val type: ParticipantTypeEnum? = null,
	@field:Parameter(description = "Visibility filter: true keeps visible elements only, false keeps hidden ones only, omitted keeps both")
	val isVisible: Boolean? = null,
	val status: PresenceStatusEnum? = null,
	@field:Parameter(description = "Instant that must fall inside the element validity window")
	@DateTimeFormat(iso = DATE_TIME)
	val at: ZonedDateTime? = null,
	@field:Parameter(description = "Account filter: true keeps participants linked to a user only, false keeps the others only, omitted keeps both")
	val hasUser: Boolean? = null,
	@field:Parameter(description = "On-site filter: true keeps participants that arrived and have not left yet, false keeps the others, omitted keeps both")
	val onSite: Boolean? = null,
	@field:Parameter(description = "Earliest birthday (inclusive), ISO date. Participants without birthday are excluded")
	@DateTimeFormat(iso = DATE)
	val birthdayFrom: LocalDate? = null,
	@field:Parameter(description = "Latest birthday (inclusive), ISO date. Participants without birthday are excluded")
	@DateTimeFormat(iso = DATE)
	val birthdayTo: LocalDate? = null,
	@field:Parameter(description = "Sort field, prefixed with - for descending order. Supported: firstName, lastName, birthday, type, startAvailability, endAvailability, status (OVERDUE, OUT, IN, UNAVAILABLE)")
	@field:Size(max = SORT_MAX, message = PARAMETER_TOO_LONG)
	val sort: String? = null,
): PageQueryDto(page, size)
