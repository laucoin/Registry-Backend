package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import java.time.ZonedDateTime
import java.util.UUID

@JsonInclude(NON_NULL)
data class EventProfileReaderDto(
    var id: UUID? = null,
    var event: EventModel? = null,
    var user: PartialUserReaderDto? = null,
    var role: String? = null,
    var status: ProfileStatusEnum? = INVITED,
    var startAccess: ZonedDateTime? = null,
    var endAccess: ZonedDateTime? = null,
    var visible: Boolean = true,
    var creation: HistoryModel = HistoryModel(),
    var lastEdition: HistoryModel = HistoryModel(),
)
