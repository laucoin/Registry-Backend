package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto

data class ProjectReaderDto(
    var name: String? = null,
    var begin: CustomDateTimeModel? = null,
    var end: CustomDateTimeModel? = null,
    var options: List<LabelDto>? = emptyList(),
): GenericReaderDto()
