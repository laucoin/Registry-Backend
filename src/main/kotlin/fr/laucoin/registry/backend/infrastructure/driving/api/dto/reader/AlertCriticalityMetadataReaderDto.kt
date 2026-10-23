package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto

data class AlertCriticalityMetadataReaderDto(
	var levels: List<LabelDto>,
	var rule: String,
)
