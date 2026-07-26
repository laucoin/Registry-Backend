package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import java.util.UUID

/**
 * A home-dashboard row: a project the caller can access that has open
 * alerts, with the count. Minimal on purpose (id/name to link + the count).
 */
data class OpenAlertProjectReaderDto(
	var id: UUID? = null,
	var name: String? = null,
	var openAlertCount: Long = 0,
)
