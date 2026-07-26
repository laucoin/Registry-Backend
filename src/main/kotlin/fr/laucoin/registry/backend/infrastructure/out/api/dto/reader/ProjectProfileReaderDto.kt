package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto

data class ProjectProfileReaderDto(
	var user: PartialUserReaderDto? = null,
	var role: LabelDto? = null,
	var availabilityStatus: LabelDto? = null,
	var status: LabelDto? = null,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
	var favorite: Boolean = false,
) : GenericProjectReaderDto()
