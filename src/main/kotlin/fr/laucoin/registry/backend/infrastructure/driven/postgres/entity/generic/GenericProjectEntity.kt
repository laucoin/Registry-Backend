package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import java.util.UUID
import org.springframework.data.relational.core.mapping.Column

abstract class GenericProjectEntity(
	@Column(LINKED_PROJECT_ID)
	var projectId: UUID? = null,
): GenericEntity()
