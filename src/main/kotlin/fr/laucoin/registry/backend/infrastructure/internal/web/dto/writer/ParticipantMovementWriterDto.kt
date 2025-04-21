package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.BothCannotBeDefined
import fr.laucoin.registry.backend.domain.annotation.MovementReason
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_AND_REASON_ARE_DEFINED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_AND_REASON_ARE_INCOMPATIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_NULL
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime
import java.util.UUID

@BothCannotBeDefined(first = "reason", second = "activityId", message = MOVEMENT_ACTIVITY_AND_REASON_ARE_DEFINED)
@MovementReason(participantType = REGISTERED, hasActivity = true, message = MOVEMENT_TYPE_AND_REASON_ARE_INCOMPATIBLE)
data class ParticipantMovementWriterDto(
    @field:NotNull(message = MOVEMENT_DATETIME_NULL)
    var dateTime: ZonedDateTime?,
    @field:NotNull(message = MOVEMENT_TYPE_NULL)
    var type: MovementTypeEnum?,
    var reason: MovementReasonEnum?,
    var activityId: UUID?,
    @field:Valid
    @field:NotEmpty(message = MOVEMENT_CONTENT_EMPTY)
    var content: List<ParticipantMovementContentWriterDto>?,
) {
    data class ParticipantMovementContentWriterDto(
        override var participantId: UUID? = null,
        var vehicleId: UUID? = null,
        var poolName: String? = null,
    ): MovementContentWriterDto(participantId = participantId)
}
