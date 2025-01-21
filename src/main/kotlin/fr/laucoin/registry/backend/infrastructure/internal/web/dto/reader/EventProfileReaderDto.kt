package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.ZonedDateTime
import java.util.UUID

@JsonInclude(NON_NULL)
data class EventProfileReaderDto(
    var id: UUID? = null,
    var event: EventReaderDto? = null,
    var user: PartialUserReaderDto? = null,
    var role: LabelDto? = null,
    var status: LabelDto? = null,
    var startAccess: ZonedDateTime? = null,
    var endAccess: ZonedDateTime? = null,
    var visible: Boolean = true,
    var creation: HistoryModel = HistoryModel(),
    var lastEdition: HistoryModel = HistoryModel(),
)
