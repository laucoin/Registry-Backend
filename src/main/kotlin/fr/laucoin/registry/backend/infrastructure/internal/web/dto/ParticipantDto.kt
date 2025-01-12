package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_START_LATER_THAN_END
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@StartBeforeEnd(startField = "begin", endField = "end", message = PARTICIPANT_START_LATER_THAN_END)
data class ParticipantDto(
    @field:NotBlank(message = PARTICIPANT_FIRST_NAME_BLANK)
    @field:Size(max = 150, message = PARTICIPANT_FIRST_NAME_TOO_LONG)
    var firstName: String? = null,
    @field:NotBlank(message = PARTICIPANT_LAST_NAME_BLANK)
    @field:Size(max = 150, message = PARTICIPANT_LAST_NAME_TOO_LONG)
    var lastName: String? = null,
    @field:PastOrPresent(message = PARTICIPANT_BIRTHDAY_FUTURE)
    var birthday: LocalDate? = null,
    var userId: UUID? = null,
    var groupIds: List<UUID> = emptyList(),
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
)
