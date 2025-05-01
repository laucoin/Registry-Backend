package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CommunicationWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class CommunicationWriterDtoMapper: IGenericProjectWriterDtoMapper<CommunicationModel, CommunicationWriterDto> {
    override fun toModel(dto: CommunicationWriterDto, projectId: UUID): CommunicationModel {
        return CommunicationModel().apply {
            dateTime = dto.dateTime !!
            message = dto.message
            movement = if (Objects.nonNull(dto.movementId)) MovementModel().apply { id = dto.movementId } else null
            project = ProjectModel().apply { id = projectId }
        }
    }
}
