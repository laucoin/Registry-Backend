package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ErrorConst.LIST_TOO_LARGE
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.GROUP_MEMBER_IDS_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.GROUP_NAME_MAX
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.StartBeforeEnd
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID

@StartBeforeEnd(startField = "startAvailability", endField = "endAvailability", message = GROUP_START_LATER_THAN_END)
data class GroupWriterDto(
	@field:NotBlank(message = GROUP_NAME_NULL_OR_BLANK)
	@field:Size(max = GROUP_NAME_MAX, message = GROUP_NAME_TOO_LONG)
	var name: String? = null,
	@field:Valid
	var startAvailability: CustomDateTimeWriterDto? = null,
	@field:Valid
	var endAvailability: CustomDateTimeWriterDto? = null,
	@field:Valid
	@field:NotEmpty(message = GROUP_MEMBERS_EMPTY)
	@field:Size(max = GROUP_MEMBER_IDS_MAX, message = LIST_TOO_LARGE)
	var members: List<UUID>? = null,
)
