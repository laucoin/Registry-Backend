package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto.GuestWriterDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GuestWriterDtoMapper: IGenericProjectWriterDtoMapper<ParticipantModel, GuestWriterDto> {
	override fun toModel(dto: GuestWriterDto, projectId: UUID): ParticipantModel {
		return ParticipantModel().apply {
			id = dto.id
			firstName = dto.firstName
			lastName = dto.lastName
			birthday = dto.birthday
			type = GUEST
			project = ProjectModel().apply { id = projectId }
		}
	}
}
