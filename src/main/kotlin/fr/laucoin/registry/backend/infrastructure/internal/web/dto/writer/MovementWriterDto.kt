package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_PARTICIPANT_ID_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_NULL
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime
import java.util.UUID

data class MovementWriterDto(
    @field:NotNull(message = MOVEMENT_DATETIME_NULL)
    var dateTime: ZonedDateTime?,
    @field:NotNull(message = MOVEMENT_TYPE_NULL)
    var type: MovementTypeEnum?,
    @field:Valid
    @field:NotEmpty(message = MOVEMENT_CONTENT_EMPTY)
    var content: List<MovementContentWriterDto>?,
) {
    data class MovementContentWriterDto(
        @field:NotNull(message = MOVEMENT_CONTENT_PARTICIPANT_ID_NULL)
        var participantId: UUID?,
        var vehicleId: UUID? = null,
        var poolName: String? = null,
    )
}
