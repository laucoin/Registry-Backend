package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CommunicationWriterDto
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

@Component
class CommunicationWriterDtoMapper : IGenericProjectWriterDtoMapper<CommunicationModel, CommunicationWriterDto> {
	/**
	 * onBehalfOfMovement is forced to false when no movement is linked: the
	 * movement voice is only meaningful with a movementId.
	 */
	override fun toModel(dto: CommunicationWriterDto, projectId: UUID): CommunicationModel {
		return CommunicationModel().apply {
			dateTime = dto.dateTime!!
			message = dto.message
			onBehalfOfMovement = dto.onBehalfOfMovement == true && dto.movementId != null
			movement = Optional.ofNullable(dto.movementId).map { MovementModel().apply { id = it } }.orElse(null)
			alert = Optional.ofNullable(dto.alertId).map { AlertModel().apply { id = it } }.orElse(null)
			project = ProjectModel().apply { id = projectId }
		}
	}
}
