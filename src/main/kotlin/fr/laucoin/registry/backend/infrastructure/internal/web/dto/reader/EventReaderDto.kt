package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.ZonedDateTime
import java.util.UUID

@JsonInclude(NON_NULL)
data class EventReaderDto(
    var id: UUID? = null,
    var name: String? = null,
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
    var options: List<LabelDto>? = emptyList(),
    var visible: Boolean = true,
    var creation: HistoryModel = HistoryModel(),
    var lastEdition: HistoryModel = HistoryModel()
)
