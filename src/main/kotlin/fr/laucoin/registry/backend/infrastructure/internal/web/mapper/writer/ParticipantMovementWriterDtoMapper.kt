package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantMovementWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ParticipantMovementWriterDtoMapper(
    private val contentMapper: ParticipantMovementContentWriterDtoMapper
): IGenericProjectWriterDtoMapper<MovementModel, ParticipantMovementWriterDto> {
    override fun toModel(dto: ParticipantMovementWriterDto, projectId: UUID): MovementModel {
        return MovementModel(contentType = REGISTERED).apply {
            dateTime = dto.dateTime !!
            type = dto.type
            reason = dto.reason
            activity = if (Objects.nonNull(dto.activityId)) ActivityModel().apply { id = dto.activityId } else null
            content = dto.content !!.map { contentMapper.toModel(it) }
            project = ProjectModel().apply { id = projectId }
        }
    }
}
