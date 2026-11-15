package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ParticipantTimelineModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantTimelineReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class, uses = [TimelineReaderDtoMapper::class])
interface ParticipantTimelineReaderDtoMapper: IGenericReaderDtoMapper<ParticipantTimelineModel, ParticipantTimelineReaderDto> {
	override fun toDto(model: ParticipantTimelineModel): ParticipantTimelineReaderDto
}
