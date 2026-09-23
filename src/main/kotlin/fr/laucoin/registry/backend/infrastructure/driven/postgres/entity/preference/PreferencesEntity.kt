package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import java.util.UUID

data class PreferencesEntity(
	var userId: UUID? = null,
	var theme: ThemeEnum = ThemeEnum.SYSTEM,
	var language: String? = null,
): GenericEntity()

