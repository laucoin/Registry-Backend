package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.annotation.AtLeastOneIsDefined
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MESSAGE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_OR_ALERT_NULL
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID

@AtLeastOneIsDefined(first = "movementId", second = "alertId", message = COMMUNICATION_MOVEMENT_OR_ALERT_NULL)
data class CommunicationWriterDto(
	@field:NotNull(message = COMMUNICATION_DATETIME_NULL)
	var dateTime: ZonedDateTime?,
	@field:Size(max = 250, message = COMMUNICATION_MESSAGE_TOO_LONG)
	var message: String? = null,
	var movementId: UUID?,
	var alertId: UUID?,
)
