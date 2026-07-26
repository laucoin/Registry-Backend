package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import java.util.UUID

data class MovementContentsReaderDto(
	var movementId: UUID? = null,
	var contents: List<MovementContentReaderDto> = emptyList(),
)
