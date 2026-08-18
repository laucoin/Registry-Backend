package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import java.util.UUID

data class OpenAlertProjectReaderDto(
	var id: UUID? = null,
	var name: String? = null,
	var openAlertCount: Long = 0,
)
