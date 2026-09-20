package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto

data class VehicleReaderDto(
	var licensePlate: String? = null,
	var brand: String? = null,
	var model: String? = null,
	var status: LabelDto? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
): GenericProjectReaderDto()
