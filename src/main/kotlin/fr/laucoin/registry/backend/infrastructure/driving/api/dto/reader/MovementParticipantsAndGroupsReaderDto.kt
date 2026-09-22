package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class MovementParticipantsAndGroupsReaderDto(
	var participants: List<ParticipantReaderDto>,
	var groups: List<GroupReaderDto>,
)
