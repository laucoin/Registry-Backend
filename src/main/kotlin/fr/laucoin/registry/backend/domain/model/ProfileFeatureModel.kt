package fr.laucoin.registry.backend.domain.model

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("registry.feature.profile")
data class ProfileFeatureModel(val searched: Searched) {
	data class Searched(val maxUserResult: Int)
}
