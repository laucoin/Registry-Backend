package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_LANGUAGE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferenceFields.PREFERENCE_THEME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.PREFERENCE_ID
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

data class CurrentUserEntity(
	@ReadOnlyProperty
	@Column(PREFERENCE_ID)
	var preferenceId: UUID? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_THEME)
	var preferenceTheme: ThemeEnum? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_LANGUAGE)
	var preferenceLanguage: String? = null,
): UserEntity()

