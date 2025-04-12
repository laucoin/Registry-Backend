package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.DateDefinedForTime
import fr.laucoin.registry.backend.domain.constant.ErrorConst.DATE_IS_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.TIME_IS_SET_BUT_NOT_DATE
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalTime

@DateDefinedForTime(dateField = "date", timeField = "time", message = TIME_IS_SET_BUT_NOT_DATE)
data class CustomDateTimeWriterDto(
    @NotNull(message = DATE_IS_NULL)
    var date: LocalDate? = null,
    var time: LocalTime? = null,
)
