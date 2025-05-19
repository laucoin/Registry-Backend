package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MESSAGE_TOO_LONG
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID

data class AlertCreationWriterDto(
    @field:NotBlank(message = ALERT_TITLE_NULL_OR_BLANK)
    @field:Size(max = 50, message = ALERT_TITLE_TOO_LONG)
    var title: String? = null,
    @field:NotNull(message = COMMUNICATION_DATETIME_NULL)
    var dateTime: ZonedDateTime?,
    @field:Size(max = 250, message = COMMUNICATION_MESSAGE_TOO_LONG)
    var message: String? = null,
    var movementId: UUID?,
)
