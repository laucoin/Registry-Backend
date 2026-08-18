package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import jakarta.validation.constraints.NotNull

data class PreferencesThemeWriterDto(
	@field:NotNull(message = PARAMETER_TYPE_MISMATCH)
	var theme: ThemeEnum? = null,
)
