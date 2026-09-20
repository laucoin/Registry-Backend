package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import java.time.LocalDate
import java.time.OffsetTime
import java.util.UUID

data class PreferencesEntity(
	var userId: UUID? = null,
	var theme: ThemeEnum = ThemeEnum.SYSTEM,
	var language: String? = null,
	var selectedProfileId: UUID? = null,
	var selectedProfileProjectId: UUID? = null,
	var selectedProfileProjectName: String? = null,
	var selectedProfileProjectStartDate: LocalDate? = null,
	var selectedProfileProjectStartTime: OffsetTime? = null,
	var selectedProfileProjectEndDate: LocalDate? = null,
	var selectedProfileProjectEndTime: OffsetTime? = null,
	var selectedProfileProjectOptions: List<ProjectOptionEnum>? = null,
	var selectedProfileRole: String? = null,
	var selectedProfileStatus: ProfileStatusEnum? = null,
	var selectedProfileStartAccessDate: LocalDate? = null,
	var selectedProfileStartAccessTime: OffsetTime? = null,
	var selectedProfileEndAccessDate: LocalDate? = null,
	var selectedProfileEndAccessTime: OffsetTime? = null,
): GenericEntity()

