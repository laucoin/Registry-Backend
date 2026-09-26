package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import java.util.UUID

data class CurrentUserEntity(
	var preferenceId: UUID? = null,
	var preferenceTheme: ThemeEnum? = null,
	var preferenceLanguage: String? = null,
): UserEntity()

