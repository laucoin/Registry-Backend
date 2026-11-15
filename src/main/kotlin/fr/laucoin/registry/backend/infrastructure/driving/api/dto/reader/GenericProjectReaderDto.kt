package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import java.util.UUID

abstract class GenericProjectReaderDto(
	var projectId: UUID? = null,
): GenericReaderDto()
