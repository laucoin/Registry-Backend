package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import java.time.ZonedDateTime

data class AlertLastCommunicationReaderDto(
	var dateTime: ZonedDateTime,
	var message: String? = null,
)
