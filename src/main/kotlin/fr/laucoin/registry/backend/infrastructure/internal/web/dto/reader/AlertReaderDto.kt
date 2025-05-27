package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.ZonedDateTime

class AlertReaderDto(
    var title: String? = null,
    var dateTime: ZonedDateTime? = null,
    var status: LabelDto? = null,
): GenericProjectReaderDto()
