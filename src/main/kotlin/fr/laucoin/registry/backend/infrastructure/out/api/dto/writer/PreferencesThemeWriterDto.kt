package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import jakarta.validation.constraints.NotNull

/**
 * API v2 Preferences theme action (ADR 017 §3: the body carries only the
 * value being set, never the action name).
 */
data class PreferencesThemeWriterDto(
	@field:NotNull(message = PARAMETER_TYPE_MISMATCH)
	var theme: ThemeEnum? = null,
)
