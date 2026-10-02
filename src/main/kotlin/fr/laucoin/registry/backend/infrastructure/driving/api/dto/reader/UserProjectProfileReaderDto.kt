package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonProperty
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto

data class UserProjectProfileReaderDto(
	var role: LabelDto? = null,
	var availabilityStatus: LabelDto? = null,
	var status: LabelDto? = null,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
	@get:JsonProperty("favorite")
	@param:JsonProperty("favorite")
	var isFavorite: Boolean = false,
	var project: ProjectReaderDto? = null,
) : GenericReaderDto()
