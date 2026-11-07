package fr.laucoin.registry.backend.domain.model

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("registry.feature.purge")
data class PurgeFeatureModel(
	val users: Threshold,
	val projects: Projects,
	val audit: Threshold,
) {
	data class Threshold(val monthsThreshold: Long)

	data class Projects(
		val monthsThreshold: Long,
		val content: Threshold,
		val configuration: Threshold,
	)
}
