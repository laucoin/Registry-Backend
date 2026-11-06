package fr.laucoin.registry.backend.domain.model

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("registry.feature.communication")
data class CommunicationFeatureModel(val searched: Searched) {
	data class Searched(val maxActivityResult: Int, val maxAlertResult: Int)
}
