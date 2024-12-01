package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import fr.laucoin.registry.backend.domain.enumeration.UserTypeEnum
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class CurrentUserDto(
    var id: UUID? = null,
    var authorities: List<String>,
    var preferences: PreferencesModel?,
    var oidcId: UUID?,
    var type: UserTypeEnum,
    var firstName: String?,
    var lastName: String?,
    var email: String?,
    var role: String?,
    var birthday: LocalDate?,
    var lastLogin: ZonedDateTime?,
    var purged: Boolean,
    var visible: Boolean,
    var creation: HistoryModel,
    var lastEdition: HistoryModel,
)
