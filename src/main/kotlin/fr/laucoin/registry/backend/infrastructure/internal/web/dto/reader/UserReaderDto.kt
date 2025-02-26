package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

data class UserReaderDto(
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var role: LabelDto? = null,
    var birthday: LocalDate? = null,
    var lastLogin: ZonedDateTime? = now(),
    var purged: Boolean = false,
): GenericReaderDto()
