package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto

data class ProjectReaderDto(
	var name: String? = null,
	var status: LabelDto? = null,
	var begin: CustomDateTimeModel? = null,
	var end: CustomDateTimeModel? = null,
	var options: List<LabelDto>? = emptyList(),
) : GenericReaderDto()
