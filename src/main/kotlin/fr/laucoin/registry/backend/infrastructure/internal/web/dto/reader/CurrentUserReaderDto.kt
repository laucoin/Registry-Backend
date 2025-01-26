package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@JsonInclude(NON_NULL)
data class CurrentUserReaderDto(
    var id: UUID? = null,
    var authorities: List<String>,
    var preferences: PreferencesModel?,
    var firstName: String?,
    var lastName: String?,
    var email: String?,
    var role: LabelDto?,
    var birthday: LocalDate?,
    var lastLogin: ZonedDateTime?,
    var purged: Boolean,
    var visible: Boolean,
    var creation: HistoryModel,
    var lastEdition: HistoryModel,
)
