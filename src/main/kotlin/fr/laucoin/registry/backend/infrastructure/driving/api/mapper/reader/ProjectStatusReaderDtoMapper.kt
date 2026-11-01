package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectStatusReaderDto.ParticipantStatusReaderDto
import org.springframework.stereotype.Component

@Component
class ProjectStatusReaderDtoMapper : IGenericReaderDtoMapper<ProjectStatusModel, ProjectStatusReaderDto> {
	override fun toDto(model: ProjectStatusModel): ProjectStatusReaderDto {
		return ProjectStatusReaderDto(
			registered = ParticipantStatusReaderDto(
				presentMinors = model.registered.presentMinors,
				presentMajors = model.registered.presentMajors,
				absentMinors = model.registered.absentMinors,
				absentMajors = model.registered.absentMajors,
			),
			guests = model.guests,
			lastRefresh = model.lastRefresh,
		)
	}
}
