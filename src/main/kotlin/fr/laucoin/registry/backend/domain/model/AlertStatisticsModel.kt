package fr.laucoin.registry.backend.domain.model

data class AlertStatisticsModel(
	val resolvedCount: Long = 0,
	val canceledCount: Long = 0,
	val averageResolutionSeconds: Long? = null,
) {
	val realAlertRate: Double?
		get() = (resolvedCount + canceledCount).takeIf { it > 0 }?.let { resolvedCount.toDouble() / it }
}
