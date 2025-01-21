package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import java.time.ZonedDateTime
import java.util.UUID

@JsonInclude(NON_NULL)
data class MovementReaderDto(
    var id: UUID? = null,
    var event: EventModel? = null,
    var dateTime: ZonedDateTime = ZonedDateTime.now(),
    var type: MovementTypeEnum? = null,
    var content: List<MovementContentReaderDto> = emptyList(),
    var visible: Boolean = true,
    var creation: HistoryModel = HistoryModel(),
    var lastEdition: HistoryModel = HistoryModel()
) {
    data class MovementContentReaderDto(
        var participant: ParticipantReaderDto? = null,
    )
}
