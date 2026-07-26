package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import java.util.UUID

data class PreferencesModel(
	var userId: UUID? = null,
	var theme: ThemeEnum = ThemeEnum.SYSTEM,
	var language: String? = null,
	var selectedProfile: ProjectProfileModel? = null
) : GenericModel()
