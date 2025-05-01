package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectWriterDto
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class ProjectWriterDtoMapper(
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericWriterDtoMapper<ProjectModel, ProjectWriterDto> {
    override fun toModel(dto: ProjectWriterDto): ProjectModel {
        return ProjectModel().apply {
            name = dto.name
            begin = if (Objects.nonNull(dto.begin)) customDateTimeMapper.toModel(dto.begin !!) else null
            end = if (Objects.nonNull(dto.end)) customDateTimeMapper.toModel(dto.end !!) else null
            options = dto.options
        }
    }
}
