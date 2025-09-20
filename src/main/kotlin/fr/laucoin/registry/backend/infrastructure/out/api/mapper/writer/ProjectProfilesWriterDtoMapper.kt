package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfilesWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ProjectProfilesWriterDtoMapper(private val customDateTimeMapper: CustomDateTimeWriterDtoMapper) {
	fun toModels(dto: ProjectProfilesWriterDto, projectId: UUID): List<ProjectProfileModel> {
		return dto.userIds!!.map { userId ->
			ProjectProfileModel().apply {
				role = dto.role
				startAccess = Optional.ofNullable(dto.startAccess).map { customDateTimeMapper.toModel(it) }.orElse(null)
				endAccess = Optional.ofNullable(dto.endAccess).map { customDateTimeMapper.toModel(it) }.orElse(null)
				project = ProjectModel().apply { id = projectId }
				user = UserModel().apply { id = userId }
			}
		}.toList()
	}
}
