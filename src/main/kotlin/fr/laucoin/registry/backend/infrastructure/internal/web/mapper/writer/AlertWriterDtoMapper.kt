package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.AlertWriterDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class AlertWriterDtoMapper: IGenericProjectWriterDtoMapper<AlertModel, AlertWriterDto> {
    override fun toModel(
        dto: AlertWriterDto,
        projectId: UUID
    ): AlertModel {
        return AlertModel().apply {
            title = dto.title
            dateTime = dto.dateTime !!
            status = dto.status
            project = ProjectModel().apply { id = projectId }
        }
    }
}
