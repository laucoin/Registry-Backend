package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import java.time.ZonedDateTime

data class CommunicationReaderDto(
    var dateTime: ZonedDateTime = ZonedDateTime.now(),
    var message: String? = null,
    var movement: MovementReaderDto? = null,
): GenericProjectReaderDto()
