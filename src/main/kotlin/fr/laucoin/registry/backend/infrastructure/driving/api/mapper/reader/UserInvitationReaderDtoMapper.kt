package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserInvitationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserInvitationReaderDto.InvitationProjectReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class UserInvitationReaderDtoMapper(
	private val profileMapper: ProjectProfileReaderDtoMapper,
): IGenericReaderDtoMapper<ProjectProfileModel, UserInvitationReaderDto> {
	override fun toDto(model: ProjectProfileModel): UserInvitationReaderDto {
		val profile = profileMapper.toDto(model)
		return UserInvitationReaderDto(
			project = Optional.ofNullable(model.project).map {
				InvitationProjectReaderDto(it.id, it.name, it.begin, it.end)
			}.orElse(null),
			role = profile.role,
			availabilityStatus = profile.availabilityStatus,
			status = profile.status,
			startAccess = profile.startAccess,
			endAccess = profile.endAccess,
		).apply {
			id = profile.id
			projectId = profile.projectId
			visible = profile.visible
			creation = profile.creation
			lastEdition = profile.lastEdition
		}
	}
}
