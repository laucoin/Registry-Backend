package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

@JsonInclude(NON_NULL)
data class GroupReaderDto(
    var members: List<ParticipantReaderDto> = emptyList(),
): GroupWithoutMemberReaderDto()
