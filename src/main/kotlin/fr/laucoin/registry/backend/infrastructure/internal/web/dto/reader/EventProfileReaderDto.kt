package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto

data class EventProfileReaderDto(
    var user: PartialUserReaderDto? = null,
    var role: LabelDto? = null,
    var status: LabelDto? = null,
    var startAccess: CustomDateTimeModel? = null,
    var endAccess: CustomDateTimeModel? = null,
): GenericEventReaderDto()
