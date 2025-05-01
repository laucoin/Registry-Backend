package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MESSAGE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NULL
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID

data class CommunicationWriterDto(
    @field:NotNull(message = COMMUNICATION_DATETIME_NULL)
    var dateTime: ZonedDateTime?,
    @field:Size(max = 250, message = COMMUNICATION_MESSAGE_TOO_LONG)
    var message: String? = null,
    @field:NotNull(message = COMMUNICATION_MOVEMENT_NULL)
    var movementId: UUID?,
)
