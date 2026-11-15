package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_TITLE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MESSAGE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ALERT_MESSAGE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.ALERT_TITLE_MAX
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime
import java.util.UUID

data class AlertCreationWriterDto(
	@field:NotBlank(message = ALERT_TITLE_NULL_OR_BLANK)
	@field:Size(max = ALERT_TITLE_MAX, message = ALERT_TITLE_TOO_LONG)
	var title: String? = null,
	@field:NotNull(message = COMMUNICATION_DATETIME_NULL)
	var dateTime: ZonedDateTime?,
	@field:Size(max = ALERT_MESSAGE_MAX, message = COMMUNICATION_MESSAGE_TOO_LONG)
	var message: String? = null,
	var movementId: UUID?,
)
