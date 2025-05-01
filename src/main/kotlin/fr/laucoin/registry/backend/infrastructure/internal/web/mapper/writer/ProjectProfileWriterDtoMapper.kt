package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectProfileWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ProjectProfileWriterDtoMapper(
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericProjectWriterDtoMapper<ProjectProfileModel, ProjectProfileWriterDto> {
    override fun toModel(dto: ProjectProfileWriterDto, projectId: UUID): ProjectProfileModel {
        return ProjectProfileModel().apply {
            role = dto.role
            startAccess = if (Objects.nonNull(dto.startAccess)) customDateTimeMapper.toModel(dto.startAccess !!) else null
            endAccess = if (Objects.nonNull(dto.endAccess)) customDateTimeMapper.toModel(dto.endAccess !!) else null
            project = ProjectModel().apply { id = projectId }
        }
    }
}
