package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum

data class PreferencesReaderDto(
	var theme: ThemeEnum = ThemeEnum.SYSTEM,
	var language: String? = null,
): GenericReaderDto()
