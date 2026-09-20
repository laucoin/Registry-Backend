package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import java.time.LocalDate
import java.time.OffsetTime
import java.util.UUID

data class CurrentUserEntity(
	var preferenceId: UUID? = null,
	var preferenceTheme: ThemeEnum? = null,
	var preferenceLanguage: String? = null,
	var preferenceSelectedProfileId: UUID? = null,
	var preferenceSelectedProfileRole: String? = null,
	var preferenceSelectedProfileStatus: ProfileStatusEnum? = null,
	var preferenceSelectedProfileStartAccessDate: LocalDate? = null,
	var preferenceSelectedProfileStartAccessTime: OffsetTime? = null,
	var preferenceSelectedProfileEndAccessDate: LocalDate? = null,
	var preferenceSelectedProfileEndAccessTime: OffsetTime? = null,
	var preferenceSelectedProfileProjectId: UUID? = null,
	var preferenceSelectedProfileProjectName: String? = null,
	var preferenceSelectedProfileProjectStartDate: LocalDate? = null,
	var preferenceSelectedProfileProjectStartTime: OffsetTime? = null,
	var preferenceSelectedProfileProjectEndDate: LocalDate? = null,
	var preferenceSelectedProfileProjectEndTime: OffsetTime? = null,
	var preferenceSelectedProfileProjectOptions: List<ProjectOptionEnum>? = null,
): UserEntity()

