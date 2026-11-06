package fr.laucoin.registry.backend.domain.model

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("registry.feature.participant")
data class ParticipantFeatureModel(val searched: Searched) {
	data class Searched(val maxUserResult: Int, val maxGroupResult: Int)
}
