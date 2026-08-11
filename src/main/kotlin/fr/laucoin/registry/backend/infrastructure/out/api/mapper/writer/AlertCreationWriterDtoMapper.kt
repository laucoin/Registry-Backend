package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AlertCreationWriterDto
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

@Component
class AlertCreationWriterDtoMapper : IGenericProjectWriterDtoMapper<AlertModel, AlertCreationWriterDto> {
	override fun toModel(
		dto: AlertCreationWriterDto,
		projectId: UUID
	): AlertModel {
		return AlertModel().apply {
			title = dto.title
			dateTime = dto.dateTime!!
			status = IN_PROGRESS
			communications = listOfNotNull(seedCommunication(dto, projectId))
			project = ProjectModel().apply { id = projectId }
		}
	}

	/**
	 * The opening message of the alert's thread — and it exists only if there is
	 * something to say. The message is optional on the form, but the alert was
	 * seeded with a communication unconditionally, so raising an alert without
	 * one wrote a blank row into the thread: an author, a timestamp and no text,
	 * which then had to be read past on every later message.
	 *
	 * A blank string counts as absent — a stray space is not a message. Dropping
	 * the communication also drops the movement it would have carried, which is
	 * correct: that link IS the message ("this outing, this note"), and there is
	 * nothing to attach a movement to without one.
	 */
	private fun seedCommunication(dto: AlertCreationWriterDto, projectId: UUID): CommunicationModel? {
		val message = dto.message?.trim()
		if (message.isNullOrEmpty()) {
			return null
		}
		return CommunicationModel().apply {
			dateTime = dto.dateTime!!
			this.message = message
			movement = Optional.ofNullable(dto.movementId).map { MovementModel().apply { id = dto.movementId } }
				.orElse(null)
			project = ProjectModel().apply { id = projectId }
		}
	}
}
