package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import java.util.UUID

/**
 * Mirrors PreferencesModel field-for-field (including the GenericModel fields
 * and the raw selectedProfile shape) so the v2 Preferences responses keep the
 * exact JSON shape clients already consume.
 */
data class PreferencesReaderDto(
	var id: UUID? = null,
	var visible: Boolean = true,
	var creation: HistoryModel? = null,
	var lastEdition: HistoryModel? = null,
	var userId: UUID? = null,
	var theme: ThemeEnum = ThemeEnum.SYSTEM,
	var language: String? = null,
	var selectedProfile: ProjectProfileModel? = null,
)
