package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_STATUS_NULL
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import jakarta.validation.constraints.NotNull

data class AlertStatusWriterDto(
	@field:NotNull(message = ALERT_STATUS_NULL)
	var status: AlertStatusEnum?,
)
