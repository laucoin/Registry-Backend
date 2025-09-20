package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_PARTICIPANT_ID_NULL
import jakarta.validation.constraints.NotNull
import java.util.UUID

open class MovementContentWriterDto(
	@field:NotNull(message = MOVEMENT_CONTENT_PARTICIPANT_ID_NULL)
	open var participantId: UUID? = null,
)
