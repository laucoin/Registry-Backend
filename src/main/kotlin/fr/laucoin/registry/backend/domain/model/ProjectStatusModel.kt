package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

data class ProjectStatusModel(
    val registered: ParticipantStatusModel,
    val guests: Long,
    val lastRefresh: ZonedDateTime = now()
) {
    data class ParticipantStatusModel(
        var presentMinors: Long,
        var presentMajors: Long,
        var absentMinors: Long,
        var absentMajors: Long,
    )
}
