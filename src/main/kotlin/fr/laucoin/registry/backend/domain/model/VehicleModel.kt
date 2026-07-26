package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import java.time.ZonedDateTime

data class VehicleModel(
	var licensePlate: String? = null,
	var brand: String? = null,
	var model: String? = null,
	var status: PresenceStatusEnum? = null,
	var lastMovement: ZonedDateTime? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
) : GenericProjectModel()
