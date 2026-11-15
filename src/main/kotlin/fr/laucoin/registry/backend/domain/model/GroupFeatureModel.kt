package fr.laucoin.registry.backend.domain.model

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("registry.feature.group")
data class GroupFeatureModel(val searched: Searched) {
	data class Searched(val maxParticipantResult: Int)
}
