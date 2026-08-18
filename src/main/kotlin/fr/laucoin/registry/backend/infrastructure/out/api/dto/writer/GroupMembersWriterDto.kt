package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_COLLECTION_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_SIZE_IS_UPPER_THAN_MAX
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID

data class GroupMembersWriterDto(
	@field:NotEmpty(message = GROUP_MEMBERS_EMPTY)
	@field:Size(max = DEFAULT_COLLECTION_LIMIT, message = GROUP_MEMBERS_SIZE_IS_UPPER_THAN_MAX)
	var participantIds: List<UUID>? = null,
)
