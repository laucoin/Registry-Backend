package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import kotlin.time.Duration

data class ActivityModel(
	var name: String? = null,
	var status: AvailabilityStatusEnum? = null,
	var description: String? = null,
	var duration: Duration? = null,
	var allowedParticipants: NumericRangeModel? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
): GenericProjectModel()
