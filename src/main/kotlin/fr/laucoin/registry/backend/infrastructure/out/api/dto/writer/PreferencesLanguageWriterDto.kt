package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import jakarta.validation.constraints.NotBlank

data class PreferencesLanguageWriterDto(
	@field:NotBlank(message = PARAMETER_TYPE_MISMATCH)
	var language: String? = null,
)
