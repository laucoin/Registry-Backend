package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_VISIBLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_ROLE
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

data class ProjectProfileRoleEntity(
	@Column(LINKED_PROJECT_ID)
	var projectId: UUID? = null,
	@ReadOnlyProperty
	@Column(LINKED_PROJECT_OPTIONS)
	var projectOptions: List<ProjectOptionEnum>? = null,
	@ReadOnlyProperty
	@Column(LINKED_PROJECT_VISIBLE)
	var projectVisible: Boolean? = null,
	@Column(PROJECT_PROFILE_ROLE)
	var role: String? = null,
)
