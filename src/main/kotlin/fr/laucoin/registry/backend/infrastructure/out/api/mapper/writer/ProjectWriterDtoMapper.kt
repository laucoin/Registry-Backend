package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectWriterDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class ProjectWriterDtoMapper(
	private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
) : IGenericWriterDtoMapper<ProjectModel, ProjectWriterDto> {
	override fun toModel(dto: ProjectWriterDto): ProjectModel {
		return ProjectModel().apply {
			name = dto.name
			begin = Optional.ofNullable(dto.begin).map(customDateTimeMapper::toModel).orElse(null)
			end = Optional.ofNullable(dto.end).map(customDateTimeMapper::toModel).orElse(null)
			options = dto.options
		}
	}
}
