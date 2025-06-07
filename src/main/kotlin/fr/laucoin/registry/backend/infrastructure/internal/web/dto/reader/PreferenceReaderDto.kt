package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum

@JsonInclude(NON_NULL)
data class PreferenceReaderDto(
    var theme: ThemeEnum = ThemeEnum.SYSTEM,
    var language: String? = null,
    var selectedProfile: ProjectProfileReaderDto? = null
)
