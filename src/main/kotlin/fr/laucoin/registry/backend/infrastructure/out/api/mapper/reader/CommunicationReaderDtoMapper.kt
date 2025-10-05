package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class CommunicationReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val movementMapper: MovementReaderDtoMapper,
	private val alertMapper: AlertReaderDtoMapper,
):
	IGenericReaderDtoMapper<CommunicationModel, CommunicationReaderDto> {
	override fun toDto(model: CommunicationModel): CommunicationReaderDto {
		return CommunicationReaderDto(
			dateTime = model.dateTime,
			message = model.message,
			movement = Optional.ofNullable(model.movement).map(movementMapper::toDto).orElse(null),
			alert = Optional.ofNullable(model.alert).map(alertMapper::toDto).orElse(null),
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map(projectMapper::toDto).orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
