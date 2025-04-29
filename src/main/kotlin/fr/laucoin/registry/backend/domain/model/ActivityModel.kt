package fr.laucoin.registry.backend.domain.model

import kotlin.time.Duration

data class ActivityModel(
    var name: String? = null,
    var description: String? = null,
    var duration: Duration? = null,
    var allowedParticipants: NumericRangeModel? = null,
    var startAvailability: CustomDateTimeModel? = null,
    var endAvailability: CustomDateTimeModel? = null,
): GenericProjectModel()
