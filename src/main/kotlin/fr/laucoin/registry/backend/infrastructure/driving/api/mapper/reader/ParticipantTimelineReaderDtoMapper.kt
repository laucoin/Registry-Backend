package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ParticipantTimelineModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantTimelineReaderDto
import org.springframework.stereotype.Component

@Component
class ParticipantTimelineReaderDtoMapper(
	private val timelineMapper: TimelineReaderDtoMapper,
): IGenericReaderDtoMapper<ParticipantTimelineModel, ParticipantTimelineReaderDto> {
	override fun toDto(model: ParticipantTimelineModel): ParticipantTimelineReaderDto {
		return ParticipantTimelineReaderDto(
			planned = model.planned.map(timelineMapper::toDto),
			actual = model.actual.map(timelineMapper::toDto),
		)
	}
}
