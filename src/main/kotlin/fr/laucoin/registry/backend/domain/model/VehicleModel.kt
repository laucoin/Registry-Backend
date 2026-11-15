package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import java.time.LocalTime
import java.time.ZonedDateTime

data class VehicleModel(
	var licensePlate: String? = null,
	var brand: String? = null,
	var model: String? = null,
	var status: PresenceStatusEnum? = null,
	var lastMovement: ZonedDateTime? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
): GenericProjectModel() {
	fun usageWindow(project: ProjectModel): TimelineWindowModel? {
		val start = (startAvailability ?: project.begin)?.toZonedDateTime()
		val end = (endAvailability ?: project.end)?.toZonedDateTime(LocalTime.MAX)

		return if (start != null && end != null && end.isAfter(start)) TimelineWindowModel(start, end) else null
	}
}
