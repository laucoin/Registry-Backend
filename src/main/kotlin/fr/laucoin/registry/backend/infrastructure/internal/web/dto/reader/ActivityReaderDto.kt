package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto

data class ActivityReaderDto(
    var name: String? = null,
    var description: String? = null,
    var duration: LabelDto? = null,
    var allowedParticipants: NumericRangeModel? = null,
    var startAvailability: CustomDateTimeModel? = null,
    var endAvailability: CustomDateTimeModel? = null,
): GenericProjectReaderDto()
