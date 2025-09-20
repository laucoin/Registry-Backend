package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum

class MovementReasonsReaderDto(
	val value: String?,
	val label: String,
	val kind: MovementReasonKindEnum,
	val type: MovementTypeEnum? = null,
)
