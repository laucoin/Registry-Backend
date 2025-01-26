package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.UUID

@JsonInclude(NON_NULL)
data class UserReaderDto(
    var id: UUID? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var role: LabelDto? = null,
    var birthday: LocalDate? = null,
    var lastLogin: ZonedDateTime? = now(),
    var purged: Boolean = false,
    var visible: Boolean = true,
    var creation: HistoryModel = HistoryModel(),
    var lastEdition: HistoryModel = HistoryModel()
)
