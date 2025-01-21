package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

data class MovementParticipantsAndGroupsReaderDto(
    var participants: List<ParticipantReaderDto>,
    var groups: List<GroupReaderDto>,
)
