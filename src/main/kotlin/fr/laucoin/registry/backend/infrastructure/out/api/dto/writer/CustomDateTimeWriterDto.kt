package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.annotation.DateDefinedForTime
import fr.laucoin.registry.backend.domain.constant.ErrorConst.DATE_IS_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.TIME_IS_SET_BUT_NOT_DATE
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.OffsetTime

@DateDefinedForTime(message = TIME_IS_SET_BUT_NOT_DATE)
data class CustomDateTimeWriterDto(
	@field:NotNull(message = DATE_IS_NULL)
	var date: LocalDate? = null,
	var time: OffsetTime? = null,
)
