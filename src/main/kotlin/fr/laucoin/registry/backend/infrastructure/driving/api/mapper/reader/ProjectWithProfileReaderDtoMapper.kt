package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectCurrentProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
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
			counts = project.counts,
			favorite = model.favorite,
			profile = Optional.ofNullable(model.profile).map(profileMapper::toDto).map(::toCurrentProfile).orElse(null),
		).apply {
			id = project.id
			visible = project.visible
			creation = project.creation
			lastEdition = project.lastEdition
		}
	}

	private fun toCurrentProfile(profile: ProjectProfileReaderDto): ProjectCurrentProfileReaderDto {
		return ProjectCurrentProfileReaderDto(
			role = profile.role,
			availabilityStatus = profile.availabilityStatus,
			status = profile.status,
			startAccess = profile.startAccess,
			endAccess = profile.endAccess,
		).apply {
			id = profile.id
			visible = profile.visible
		}
	}
}
