package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CommunicationWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class CommunicationWriterDtoMapper: IGenericProjectWriterDtoMapper<CommunicationModel, CommunicationWriterDto> {
    override fun toModel(dto: CommunicationWriterDto, projectId: UUID): CommunicationModel {
        return CommunicationModel().apply {
            dateTime = dto.dateTime !!
            message = dto.message
            movement = Optional.ofNullable(dto.movementId).map { MovementModel().apply { id = it } }.orElse(null)
            alert = Optional.ofNullable(dto.alertId).map { AlertModel().apply { id = it } }.orElse(null)
            project = ProjectModel().apply { id = projectId }
        }
    }
}
