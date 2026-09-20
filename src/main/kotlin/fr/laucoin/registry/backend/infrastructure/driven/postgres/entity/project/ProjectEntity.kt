package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import java.time.LocalDate
import java.time.OffsetTime

data class ProjectEntity(
	var name: String? = null,
	var beginDate: LocalDate? = null,
	var beginTime: OffsetTime? = null,
	var endDate: LocalDate? = null,
	var endTime: OffsetTime? = null,
	var options: List<ProjectOptionEnum>? = null,
	var favorite: Boolean? = null,
): GenericEntity()
