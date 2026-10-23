package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_LANGUAGE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_THEME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import java.util.UUID
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(PREFERENCE_TABLE)
data class PreferencesEntity(
	@Column(PREFERENCE_USER_ID)
	var userId: UUID? = null,
	@Column(PREFERENCE_THEME)
	var theme: ThemeEnum = ThemeEnum.SYSTEM,
	@Column(PREFERENCE_LANGUAGE)
	var language: String? = null,
): GenericEntity()

