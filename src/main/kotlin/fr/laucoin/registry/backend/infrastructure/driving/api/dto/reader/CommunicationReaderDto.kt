package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.constant.DateTimeConst.EPOCH_UTC
import java.time.ZonedDateTime

data class CommunicationReaderDto(
	var dateTime: ZonedDateTime = EPOCH_UTC,
	var message: String? = null,
	var movement: MovementReaderDto? = null,
	var alert: AlertReaderDto? = null,
): GenericReaderDto()
