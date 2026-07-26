package fr.laucoin.registry.backend.domain.model

/**
 * A project the caller can access that currently has open (IN_PROGRESS)
 * alerts, with the count, for the home dashboard's "projects needing attention".
 */
data class OpenAlertProjectModel(
	var project: ProjectModel? = null,
	var openAlertCount: Long = 0,
)
