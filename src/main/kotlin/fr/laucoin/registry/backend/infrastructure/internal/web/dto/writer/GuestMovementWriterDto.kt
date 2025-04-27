package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.MovementGuestContent
import fr.laucoin.registry.backend.domain.annotation.MovementReason
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ODD_GUEST_CONTENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_AND_REASON_ARE_INCOMPATIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@MovementGuestContent(message = MOVEMENT_ODD_GUEST_CONTENT)
@MovementReason(participantType = GUEST, message = MOVEMENT_TYPE_AND_REASON_ARE_INCOMPATIBLE)
data class GuestMovementWriterDto(
    @field:NotNull(message = MOVEMENT_DATETIME_NULL)
    var dateTime: ZonedDateTime?,
    @field:NotNull(message = MOVEMENT_TYPE_NULL)
    var type: MovementTypeEnum?,
    var reason: MovementReasonEnum?,
    var content: List<MovementContentWriterDto>?,
    @field:Valid
    var guests: List<GuestWriterDto>?,
) {
    data class GuestWriterDto(
        var id: UUID? = null,
        @field:NotBlank(message = PARTICIPANT_FIRST_NAME_NULL_OR_BLANK)
        @field:Size(max = 150, message = PARTICIPANT_FIRST_NAME_TOO_LONG)
        var firstName: String? = null,
        @field:NotBlank(message = PARTICIPANT_LAST_NAME_NULL_OR_BLANK)
        @field:Size(max = 150, message = PARTICIPANT_LAST_NAME_TOO_LONG)
        var lastName: String? = null,
        @field:NotNull(message = PARTICIPANT_BIRTHDAY_NULL)
        @field:PastOrPresent(message = PARTICIPANT_BIRTHDAY_FUTURE)
        var birthday: LocalDate? = null,
    )
}
