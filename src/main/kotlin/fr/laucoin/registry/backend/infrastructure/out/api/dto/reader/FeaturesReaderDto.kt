package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

/**
 * Deployment-level feature switches the UI has to mirror, so a surface the API
 * would refuse is never offered. Read-only projection of the `registry.feature.*`
 * configuration — the backend stays the enforcing side.
 */
data class FeaturesReaderDto(
	var lightUser: Boolean,
)
