package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.LocalDate
import java.time.ZonedDateTime

data class CurrentUserReaderDto(
    var authorities: List<String>,
    var preferences: PreferenceReaderDto?,
    var firstName: String?,
    var lastName: String?,
    var email: String?,
    var role: LabelDto?,
    var birthday: LocalDate?,
    var lastLogin: ZonedDateTime?,
    var purged: Boolean,
): GenericReaderDto()
