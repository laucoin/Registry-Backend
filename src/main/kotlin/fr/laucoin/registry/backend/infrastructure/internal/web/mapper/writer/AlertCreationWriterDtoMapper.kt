package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.AlertCreationWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class AlertCreationWriterDtoMapper: IGenericProjectWriterDtoMapper<AlertModel, AlertCreationWriterDto> {
    override fun toModel(
        dto: AlertCreationWriterDto,
        projectId: UUID
    ): AlertModel {
        return AlertModel().apply {
            title = dto.title
            dateTime = dto.dateTime !!
            status = IN_PROGRESS
            communications = listOf(
                CommunicationModel().apply {
                    dateTime = dto.dateTime !!
                    message = dto.message
                    movement = Optional.ofNullable(dto.movementId).map { MovementModel().apply { id = dto.movementId } }.orElse(null)
                    project = ProjectModel().apply { id = projectId }
                }
            )
            project = ProjectModel().apply { id = projectId }
        }
    }
}
