package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import java.util.UUID

data class ProjectProfileRoleEntity(
	var projectId: UUID? = null,
	var projectOptions: List<ProjectOptionEnum>? = null,
	var projectVisible: Boolean? = null,
	var role: String? = null,
)
