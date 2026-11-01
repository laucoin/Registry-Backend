package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.OutingModel
import fr.laucoin.registry.backend.domain.model.TimelineRangeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.OutingReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.TimelineRangeReaderDto
import org.springframework.stereotype.Component

@Component
class TimelineReaderDtoMapper {
	fun toDto(model: TimelineRangeModel): TimelineRangeReaderDto {
		return TimelineRangeReaderDto(start = model.start, end = model.end)
	}

	fun toDto(model: OutingModel): OutingReaderDto {
		return OutingReaderDto(start = model.start, end = model.end, ongoing = model.ongoing)
	}
}
