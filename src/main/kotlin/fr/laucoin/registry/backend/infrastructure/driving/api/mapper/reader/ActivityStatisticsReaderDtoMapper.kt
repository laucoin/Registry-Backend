package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityStatisticsReaderDto
import org.springframework.stereotype.Component

@Component
class ActivityStatisticsReaderDtoMapper: IGenericReaderDtoMapper<ActivityStatisticsModel, ActivityStatisticsReaderDto> {
	override fun toDto(model: ActivityStatisticsModel): ActivityStatisticsReaderDto {
		return ActivityStatisticsReaderDto(
			participantCount = model.participantCount,
			projectParticipantCount = model.projectParticipantCount,
			participantRate = model.participantRate,
			movementSeconds = model.movementSeconds,
			manSeconds = model.manSeconds,
		)
	}
}
