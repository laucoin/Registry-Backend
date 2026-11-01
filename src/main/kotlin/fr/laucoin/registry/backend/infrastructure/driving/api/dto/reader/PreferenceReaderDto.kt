package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import java.util.UUID

@JsonInclude(NON_NULL)
data class PreferenceReaderDto(
	var userId: UUID? = null,
	var theme: ThemeEnum = ThemeEnum.SYSTEM,
	var language: String? = null,
	var refreshMode: RefreshModeEnum = RefreshModeEnum.NORMAL,
)
