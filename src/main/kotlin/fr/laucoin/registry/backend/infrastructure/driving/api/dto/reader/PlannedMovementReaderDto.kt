package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto

data class PlannedMovementReaderDto(
	var type: LabelDto? = null,
	var reason: MovementReasonsReaderDto? = null,
	var contentType: ParticipantTypeEnum,
	var content: List<MovementContentReaderDto> = emptyList(),
)
