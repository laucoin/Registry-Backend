package fr.laucoin.registry.backend.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.util.Objects
import java.util.TimeZone

data class CustomDateTimeModel(
    var date: LocalDate,
    var time: LocalTime? = null,
) {
    constructor(dateTime: LocalDateTime): this(dateTime.toLocalDate(), dateTime.toLocalTime())
    constructor(dateTime: ZonedDateTime): this(dateTime.toLocalDate(), dateTime.toLocalTime())

    companion object {
        @JsonIgnore
        fun now(): CustomDateTimeModel {
            val zonedTimeNow = ZonedDateTime.now(ZoneId.of("UTC")).toLocalTime()
            return CustomDateTimeModel(LocalDate.now(), zonedTimeNow)
        }
    }

    @JsonIgnore
    fun toLocalDateTime(defaultTime: LocalTime): LocalDateTime? {
        return when {
            Objects.isNull(date) -> null
            Objects.isNull(time) -> LocalDateTime.of(date, defaultTime)
            else -> LocalDateTime.of(date, time)
        }
    }

    @JsonIgnore
    fun toZonedDateTime(timeZone: TimeZone): ZonedDateTime? {
        return ZonedDateTime.of(toLocalDateTime(LocalTime.now()), ZoneId.of(timeZone.id))
    }

    @JsonIgnore
    override fun toString(): String {
        return when {
            Objects.isNull(date) -> ""
            Objects.isNull(time) -> date.format(ISO_LOCAL_DATE)
            else -> LocalDateTime.of(date, time).format(ISO_LOCAL_DATE_TIME)
        }
    }
}
