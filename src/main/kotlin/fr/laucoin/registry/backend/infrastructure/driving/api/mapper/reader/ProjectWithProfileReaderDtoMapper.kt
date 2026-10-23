package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectCurrentProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectWithProfileReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ProjectWithProfileReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val profileMapper: ProjectProfileReaderDtoMapper,
): IGenericReaderDtoMapper<ProjectModel, ProjectWithProfileReaderDto> {
	override fun toDto(model: ProjectModel): ProjectWithProfileReaderDto {
		val project = projectMapper.toDto(model)
		return ProjectWithProfileReaderDto(
			name = project.name,
			status = project.status,
			begin = project.begin,
			end = project.end,
			options = project.options,
			profile = Optional.ofNullable(model.profile).map(profileMapper::toDto).map {
				ProjectCurrentProfileReaderDto(
					role = it.role,
					availabilityStatus = it.availabilityStatus,
					status = it.status,
					startAccess = it.startAccess,
					endAccess = it.endAccess,
				).apply {
					id = it.id
					visible = it.visible
				}
			}.orElse(null),
		).apply {
			id = project.id
			visible = project.visible
			creation = project.creation
			lastEdition = project.lastEdition
		}
	}
}
