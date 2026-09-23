package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ParticipantDataExportModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantDataExportReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ParticipantDataExportReaderDtoMapper(
	private val participantMapper: ParticipantReaderDtoMapper,
	private val movementMapper: MovementReaderDtoMapper,
	private val communicationMapper: CommunicationReaderDtoMapper,
) {
	fun toDto(model: ParticipantDataExportModel): ParticipantDataExportReaderDto {
		return ParticipantDataExportReaderDto(
			participant = Optional.ofNullable(model.participant).map(participantMapper::toDto).orElse(null),
			movements = model.movements.map(movementMapper::toDto),
			communications = model.communications.map(communicationMapper::toDto),
		)
	}
}
