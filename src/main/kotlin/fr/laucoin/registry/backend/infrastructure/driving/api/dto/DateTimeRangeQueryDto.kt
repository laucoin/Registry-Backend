package fr.laucoin.registry.backend.infrastructure.driving.api.dto

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import java.time.ZonedDateTime

class DateTimeRangeQueryDto {
	@field:DateTimeFormat(iso = DATE_TIME)
	var startDateTime: ZonedDateTime? = null

	@field:DateTimeFormat(iso = DATE_TIME)
	var endDateTime: ZonedDateTime? = null
}
