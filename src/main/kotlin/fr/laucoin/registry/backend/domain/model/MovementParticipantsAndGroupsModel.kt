package fr.laucoin.registry.backend.domain.model

data class MovementParticipantsAndGroupsModel(
    var participants: List<ParticipantModel>,
    var groups: List<GroupModel>,
)
