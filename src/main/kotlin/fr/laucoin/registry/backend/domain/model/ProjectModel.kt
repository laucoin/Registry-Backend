package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isInRange

data class ProjectModel(
	var name: String? = null,
	var status: AvailabilityStatusEnum? = null,
	var begin: CustomDateTimeModel? = null,
	var end: CustomDateTimeModel? = null,
	var options: List<ProjectOptionEnum>? = emptyList(),
	var counts: ProjectCountsModel? = null,
	var activeProfile: ProjectProfileModel? = null,
) : GenericModel() {
	data class ProjectCountsModel(
		var participants: Long = 0,
		var vehicles: Long = 0,
		var groups: Long = 0,
		var activities: Long = 0,
		var profiles: Long = 0,
		var ongoingAlerts: Long = 0,
	)

	fun isNotInRange(dateTime: CustomDateTimeModel?): Boolean {
		return dateTime.isInRange(begin, end).not()
	}
}
