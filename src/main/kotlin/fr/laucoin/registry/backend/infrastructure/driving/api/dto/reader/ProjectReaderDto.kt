package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto

data class ProjectReaderDto(
	var name: String? = null,
	var status: LabelDto? = null,
	var begin: CustomDateTimeModel? = null,
	var end: CustomDateTimeModel? = null,
	var options: List<LabelDto>? = emptyList(),
	var favorite: Boolean = false,
): GenericReaderDto()
