package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfileWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ProjectProfileWriterDtoMapper(
	private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericProjectWriterDtoMapper<ProjectProfileModel, ProjectProfileWriterDto> {
	override fun toModel(dto: ProjectProfileWriterDto, projectId: UUID): ProjectProfileModel {
		return ProjectProfileModel().apply {
			role = dto.role
			startAccess = Optional.ofNullable(dto.startAccess).map { customDateTimeMapper.toModel(it) }.orElse(null)
			endAccess = Optional.ofNullable(dto.endAccess).map { customDateTimeMapper.toModel(it) }.orElse(null)
			project = ProjectModel().apply { id = projectId }
		}
	}
}
