package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectProfilesWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ProjectProfilesWriterDtoMapper(private val customDateTimeMapper: CustomDateTimeWriterDtoMapper) {
    fun toModels(dto: ProjectProfilesWriterDto, projectId: UUID): List<ProjectProfileModel> {
        return dto.userIds !!.map {
            ProjectProfileModel().apply {
                role = dto.role
                startAccess = if (Objects.nonNull(dto.startAccess)) customDateTimeMapper.toModel(dto.startAccess !!) else null
                endAccess = if (Objects.nonNull(dto.endAccess)) customDateTimeMapper.toModel(dto.endAccess !!) else null
                project = ProjectModel().apply { id = projectId }
                user = UserModel().apply { id = it }
            }
        }.toList()
    }
}
