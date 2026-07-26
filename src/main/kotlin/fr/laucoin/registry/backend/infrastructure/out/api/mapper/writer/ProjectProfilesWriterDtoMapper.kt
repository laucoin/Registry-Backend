package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfilesWriterDto
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

@Component
class ProjectProfilesWriterDtoMapper(private val customDateTimeMapper: CustomDateTimeWriterDtoMapper) {
	/**
	 * Builds a single, user-less profile template (role + access window + project).
	 * The service clones it per resolved user (from `userIds` and invited `emails`),
	 * so the mapping stays synchronous while user resolution happens reactively.
	 */
	fun toTemplate(dto: ProjectProfilesWriterDto, projectId: UUID): ProjectProfileModel {
		return ProjectProfileModel().apply {
			role = dto.role
			startAccess = Optional.ofNullable(dto.startAccess).map(customDateTimeMapper::toModel).orElse(null)
			endAccess = Optional.ofNullable(dto.endAccess).map(customDateTimeMapper::toModel).orElse(null)
			project = ProjectModel().apply { id = projectId }
		}
	}
}
