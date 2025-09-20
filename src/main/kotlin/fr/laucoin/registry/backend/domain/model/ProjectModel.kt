package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum

data class ProjectModel(
	var name: String? = null,
	var status: AvailabilityStatusEnum? = null,
	var begin: CustomDateTimeModel? = null,
	var end: CustomDateTimeModel? = null,
	var options: List<ProjectOptionEnum>? = emptyList(),
): GenericModel()
