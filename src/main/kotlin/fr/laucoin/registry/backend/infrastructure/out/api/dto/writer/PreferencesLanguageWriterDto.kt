package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import jakarta.validation.constraints.NotBlank

/**
 * API v2 Preferences language action (ADR 017 §3: the body carries only the
 * value being set, never the action name).
 */
data class PreferencesLanguageWriterDto(
	@field:NotBlank(message = PARAMETER_TYPE_MISMATCH)
	var language: String? = null,
)
