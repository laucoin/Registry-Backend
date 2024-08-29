package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANT_NULL
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class MovementContentDto(
    @field:NotNull(message = MOVEMENT_PARTICIPANT_NULL)
    var participantId: UUID?,
)
