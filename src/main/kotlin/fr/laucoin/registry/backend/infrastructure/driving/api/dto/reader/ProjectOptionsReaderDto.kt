package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto

@JsonInclude(NON_NULL)
data class ProjectOptionsReaderDto(
	var value: ProjectOptionEnum,
	var label: String,
	var ask: String,
	var preRequired: List<LabelDto>,
)
