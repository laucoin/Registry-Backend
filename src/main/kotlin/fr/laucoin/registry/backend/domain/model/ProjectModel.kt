package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isEndInRange
import fr.laucoin.registry.backend.domain.extension.DateExt.isInRange
import fr.laucoin.registry.backend.domain.extension.DateExt.isStartInRange

data class ProjectModel(
	var name: String? = null,
	var status: AvailabilityStatusEnum? = null,
	var begin: CustomDateTimeModel? = null,
	var end: CustomDateTimeModel? = null,
	var options: List<ProjectOptionEnum>? = emptyList(),
) : GenericModel() {
	fun isNotInRange(dateTime: CustomDateTimeModel?): Boolean {
		return dateTime.isInRange(begin, end).not()
	}

	/**
	 * An element's own window must sit INSIDE the project's, and each end is
	 * resolved as what it is: a bare arrival date is its midnight, a bare
	 * departure date its 23:59:59. Checking both ends with the same reading is
	 * what used to let a departure on the project's closing day slip through (or
	 * be refused) depending on whether that day carried a time.
	 */
	fun isNotStartInRange(dateTime: CustomDateTimeModel?): Boolean {
		return dateTime.isStartInRange(begin, end).not()
	}

	fun isNotEndInRange(dateTime: CustomDateTimeModel?): Boolean {
		return dateTime.isEndInRange(begin, end).not()
	}
}
