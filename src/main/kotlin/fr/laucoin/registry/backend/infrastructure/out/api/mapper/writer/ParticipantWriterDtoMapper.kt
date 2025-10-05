package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ParticipantWriterDtoMapper(
	private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericProjectWriterDtoMapper<ParticipantModel, ParticipantWriterDto> {
	override fun toModel(dto: ParticipantWriterDto, projectId: UUID): ParticipantModel {
		return ParticipantModel().apply {
			firstName = dto.firstName
			lastName = dto.lastName
			birthday = dto.birthday
			type = REGISTERED
			startAvailability =
				Optional.ofNullable(dto.startAvailability).map(customDateTimeMapper::toModel).orElse(null)
			endAvailability =
				Optional.ofNullable(dto.endAvailability).map(customDateTimeMapper::toModel).orElse(null)
			user = Optional.ofNullable(dto.userId).map { UserModel().apply { id = it } }.orElse(null)
			groups =
				Optional.ofNullable(dto.groupIds).map { groups -> groups.map { GroupModel().apply { id = it } } }
					.orElse(emptyList())
			project = ProjectModel().apply { id = projectId }
		}
	}
}
