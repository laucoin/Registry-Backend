package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto

data class PlannedMovementReaderDto(
	var type: LabelDto? = null,
	var reason: MovementReasonsReaderDto? = null,
	var contentType: ParticipantTypeEnum,
	var plannedAt: CustomDateTimeModel? = null,
	var poolName: String? = null,
	var participants: List<ParticipantReaderDto> = emptyList(),
)
