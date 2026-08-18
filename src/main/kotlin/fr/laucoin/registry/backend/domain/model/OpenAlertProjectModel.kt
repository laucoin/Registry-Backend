package fr.laucoin.registry.backend.domain.model

data class OpenAlertProjectModel(
	var project: ProjectModel? = null,
	var openAlertCount: Long = 0,
)
