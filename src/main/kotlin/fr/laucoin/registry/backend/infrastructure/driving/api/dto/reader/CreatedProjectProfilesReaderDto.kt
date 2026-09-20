package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import java.util.UUID

@JsonInclude(NON_NULL)
data class CreatedProjectProfilesReaderDto(
	var createdUserIds: List<UUID>,
	var notCreatedUserIds: List<UUID>,
)
