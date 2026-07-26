package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto

data class ActivityReaderDto(
	var name: String? = null,
	var status: LabelDto? = null,
	var description: String? = null,
	var duration: LabelDto? = null,
	var allowedParticipants: NumericRangeModel? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
) : GenericProjectReaderDto()
