package fr.laucoin.registry.backend.domain.model

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("registry.feature.movement")
data class MovementFeatureModel(
	val searched: Searched,
	val planned: Planned,
	val futureTolerance: Duration,
) {
	data class Searched(
		val maxParticipantResult: Int,
		val maxGroupResult: Int,
		val maxVehicleResult: Int,
		val maxActivityResult: Int,
	)

	data class Planned(val maxResult: Int)
}
