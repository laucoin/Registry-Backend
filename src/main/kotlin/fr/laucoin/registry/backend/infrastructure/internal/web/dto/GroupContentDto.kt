package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_PARTICIPANT_NULL
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class GroupContentDto(
    @field:NotNull(message = GROUP_PARTICIPANT_NULL)
    var participantId: UUID? = null,
)
