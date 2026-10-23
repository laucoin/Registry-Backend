package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.LIST_TOO_LARGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PARTICIPANT_FIRST_NAME_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PARTICIPANT_GROUP_IDS_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PARTICIPANT_LAST_NAME_MAX
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.StartBeforeEnd
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

@StartBeforeEnd(
	startField = "startAvailability",
	endField = "endAvailability",
	message = PARTICIPANT_START_LATER_THAN_END
)
data class ParticipantWriterDto(
	@field:NotBlank(message = PARTICIPANT_FIRST_NAME_NULL_OR_BLANK)
	@field:Size(max = PARTICIPANT_FIRST_NAME_MAX, message = PARTICIPANT_FIRST_NAME_TOO_LONG)
	var firstName: String? = null,
	@field:NotBlank(message = PARTICIPANT_LAST_NAME_NULL_OR_BLANK)
	@field:Size(max = PARTICIPANT_LAST_NAME_MAX, message = PARTICIPANT_LAST_NAME_TOO_LONG)
	var lastName: String? = null,
	@field:NotNull(message = PARTICIPANT_BIRTHDAY_NULL)
	@field:PastOrPresent(message = PARTICIPANT_BIRTHDAY_FUTURE)
	var birthday: LocalDate? = null,
	var userId: UUID? = null,
	@field:Size(max = PARTICIPANT_GROUP_IDS_MAX, message = LIST_TOO_LARGE)
	var groupIds: List<UUID>? = null,
	@field:Valid
	var startAvailability: CustomDateTimeWriterDto? = null,
	@field:Valid
	var endAvailability: CustomDateTimeWriterDto? = null,
)
