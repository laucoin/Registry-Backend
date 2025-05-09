package fr.laucoin.registry.backend.domain.model

data class ProjectStatusModel(
    val registered: ParticipantStatusModel,
    val guests: Long,
) {
    data class ParticipantStatusModel(
        var presentMinors: Long,
        var presentMajors: Long,
        var absentMinors: Long,
        var absentMajors: Long,
    )
}
