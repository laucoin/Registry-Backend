package fr.laucoin.registry.backend.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.Objects


data class CustomDateTimeModel(
    var date: LocalDate,
    var time: OffsetTime? = null,
) {
    constructor(dateTime: ZonedDateTime): this(dateTime.toLocalDate(), dateTime.toOffsetDateTime().toOffsetTime())

    companion object {
        val MIN = CustomDateTimeModel(OffsetDateTime.MIN.toZonedDateTime())
        val MAX = CustomDateTimeModel(OffsetDateTime.MAX.toZonedDateTime())

        @JsonIgnore
        fun now(): CustomDateTimeModel {
            return CustomDateTimeModel(ZonedDateTime.now(ZoneId.of("UTC")))
        }
    }

    @JsonIgnore
    fun toZonedDateTime(defaultTime: OffsetTime? = null): ZonedDateTime? {
        return when {
            Objects.isNull(date) -> null
            Objects.nonNull(time) -> time !!.atDate(date).toZonedDateTime()
            Objects.nonNull(defaultTime) -> date.atTime(defaultTime).toZonedDateTime()
            else -> date.atStartOfDay(ZoneId.of("UTC"))
        }
    }

    @JsonIgnore
    override fun toString(): String {
        return when {
            Objects.isNull(date) -> ""
            Objects.isNull(time) -> date.format(ISO_LOCAL_DATE)
            else -> toZonedDateTime() !!.format(ISO_ZONED_DATE_TIME)
        }
    }
}
