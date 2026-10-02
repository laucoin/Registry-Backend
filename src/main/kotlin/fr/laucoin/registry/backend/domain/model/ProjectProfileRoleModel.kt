package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import java.util.UUID

data class ProjectProfileRoleModel(
	var projectId: UUID? = null,
	var projectOptions: List<ProjectOptionEnum>? = null,
	var isProjectVisible: Boolean? = null,
	var role: String? = null,
)
