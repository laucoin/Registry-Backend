package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import java.time.ZonedDateTime

class AlertReaderDto(
	var title: String? = null,
	var dateTime: ZonedDateTime? = null,
	var status: LabelDto? = null,
): GenericProjectReaderDto()
