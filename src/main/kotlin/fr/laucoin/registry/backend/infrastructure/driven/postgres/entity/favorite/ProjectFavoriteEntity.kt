package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_USER_ID
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(PROJECT_FAVORITE_TABLE)
data class ProjectFavoriteEntity(
	@Id
	var id: UUID? = null,
	@Column(PROJECT_FAVORITE_USER_ID)
	var userId: UUID? = null,
	@Column(PROJECT_FAVORITE_PROJECT_ID)
	var projectId: UUID? = null,
)
