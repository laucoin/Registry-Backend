package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.ZonedDateTime

data class MovementReaderDto(
    var dateTime: ZonedDateTime = ZonedDateTime.now(),
    var type: LabelDto? = null,
    var reason: MovementReasonsReaderDto? = null,
    var contentType: ParticipantTypeEnum,
    var content: List<MovementContentReaderDto> = emptyList(),
): GenericEventReaderDto() {
    data class MovementContentReaderDto(
        var poolName: String? = null,
        var participant: ParticipantReaderDto? = null,
        var vehicle: VehicleReaderDto? = null,
    )
}
