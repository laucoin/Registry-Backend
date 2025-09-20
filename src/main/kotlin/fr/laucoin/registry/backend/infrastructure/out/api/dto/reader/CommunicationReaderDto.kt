package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import java.time.ZonedDateTime

data class CommunicationReaderDto(
	var dateTime: ZonedDateTime = ZonedDateTime.now(),
	var message: String? = null,
	var movement: MovementReaderDto? = null,
	var alert: AlertReaderDto? = null,
): GenericProjectReaderDto()
