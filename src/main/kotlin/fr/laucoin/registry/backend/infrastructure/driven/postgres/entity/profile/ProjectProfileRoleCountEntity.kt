package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile

import java.util.UUID

data class ProjectProfileRoleCountEntity(
	var projectId: UUID? = null,
	var projectName: String? = null,
	var level0: Int? = null,
)
