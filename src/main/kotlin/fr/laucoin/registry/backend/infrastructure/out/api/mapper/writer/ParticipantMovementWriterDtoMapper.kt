package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ParticipantMovementWriterDtoMapper(
	private val contentMapper: ParticipantMovementContentWriterDtoMapper
): IGenericProjectWriterDtoMapper<MovementModel, ParticipantMovementWriterDto> {
	override fun toModel(dto: ParticipantMovementWriterDto, projectId: UUID): MovementModel {
		return MovementModel(contentType = REGISTERED).apply {
			dateTime = dto.dateTime!!
			type = dto.type
			reason = dto.reason
			activity = Optional.ofNullable(dto.activityId).map { ActivityModel().apply { id = it } }.orElse(null)
			content = dto.content!!.map { contentMapper.toModel(it) }
			project = ProjectModel().apply { id = projectId }
		}
	}
}
