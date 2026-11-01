package fr.laucoin.registry.backend.domain.model

data class ActivityStatisticsModel(
	val participantCount: Long = 0,
	val projectParticipantCount: Long = 0,
	val movementSeconds: Long = 0,
	val manSeconds: Long = 0,
) {
	val participantRate: Double?
		get() = projectParticipantCount.takeIf { it > 0 }?.let { participantCount.toDouble() / it }
}
