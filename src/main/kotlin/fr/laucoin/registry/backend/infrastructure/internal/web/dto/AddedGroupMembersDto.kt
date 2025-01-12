package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import java.util.UUID

@JsonInclude(NON_NULL)
data class AddedGroupMembersDto(
    var members: List<UUID>,
    var notAddedMemberIds: List<UUID>? = null,
)
