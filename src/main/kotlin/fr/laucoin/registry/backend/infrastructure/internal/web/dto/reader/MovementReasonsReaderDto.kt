package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum

class MovementReasonsReaderDto(
    val value: String?,
    val label: String,
    val kind: MovementReasonKindEnum,
    val type: MovementTypeEnum? = null,
)
