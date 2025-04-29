package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileFields.ROLE_COUNT
import java.util.UUID
import org.springframework.data.relational.core.mapping.Column

data class ProjectProfileRoleCountEntity(
    @Column(LINKED_PROJECT_ID)
    var projectId: UUID? = null,
    @Column(LINKED_PROJECT_NAME)
    var projectName: String? = null,
    @Column(ROLE_COUNT)
    var level0: Int? = null,
)
