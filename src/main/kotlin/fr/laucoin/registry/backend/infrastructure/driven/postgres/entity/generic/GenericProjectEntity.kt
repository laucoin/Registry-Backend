package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import java.time.LocalDate
import java.time.OffsetTime
import java.util.UUID

abstract class GenericProjectEntity(
	var projectId: UUID? = null,
	var projectName: String? = null,
	var projectStartDate: LocalDate? = null,
	var projectStartTime: OffsetTime? = null,
	var projectEndDate: LocalDate? = null,
	var projectEndTime: OffsetTime? = null,
	var projectOptions: List<ProjectOptionEnum>? = null,
): GenericEntity()
