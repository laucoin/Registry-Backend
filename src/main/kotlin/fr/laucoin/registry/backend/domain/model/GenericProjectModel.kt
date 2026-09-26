package fr.laucoin.registry.backend.domain.model

import java.util.UUID

abstract class GenericProjectModel(
	var projectId: UUID? = null,
): GenericModel()
