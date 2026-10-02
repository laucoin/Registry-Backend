package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.HistoryModel
import java.util.UUID

@JsonInclude(NON_NULL)
abstract class GenericReaderDto(
	var id: UUID? = null,
	@get:JsonProperty("visible")
	@param:JsonProperty("visible")
	var isVisible: Boolean = true,
	var creation: HistoryModel? = null,
	var lastEdition: HistoryModel? = null,
)
