package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum

data class AlertCriticalityMetadataModel(
	val levels: List<AlertCriticalityEnum>,
)
