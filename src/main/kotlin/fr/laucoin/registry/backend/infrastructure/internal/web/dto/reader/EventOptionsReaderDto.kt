package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto

@JsonInclude(NON_NULL)
data class EventOptionsReaderDto(
    var value: EventOptionEnum,
    var label: String,
    var ask: String,
    var preRequired: List<LabelDto>,
)
