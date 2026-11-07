package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel

data class TimelineRangeReaderDto(
	val start: CustomDateTimeModel?,
	val end: CustomDateTimeModel?,
)
