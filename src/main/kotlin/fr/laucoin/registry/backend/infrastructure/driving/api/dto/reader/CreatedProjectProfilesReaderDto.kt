package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

@JsonInclude(NON_NULL)
data class CreatedProjectProfilesReaderDto(
	var createdEmails: List<String>,
	var notCreatedEmails: List<String>,
)
