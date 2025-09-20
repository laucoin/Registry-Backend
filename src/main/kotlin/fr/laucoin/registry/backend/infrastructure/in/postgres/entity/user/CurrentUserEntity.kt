package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_LANGUAGE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_START_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_START_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferenceFields.PREFERENCE_THEME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.PREFERENCE_ID
import java.time.LocalDate
import java.time.OffsetTime
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
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_ID)
	var preferenceSelectedProfileId: UUID? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_ROLE)
	var preferenceSelectedProfileRole: String? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_STATUS)
	var preferenceSelectedProfileStatus: ProfileStatusEnum? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE)
	var preferenceSelectedProfileStartAccessDate: LocalDate? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME)
	var preferenceSelectedProfileStartAccessTime: OffsetTime? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE)
	var preferenceSelectedProfileEndAccessDate: LocalDate? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME)
	var preferenceSelectedProfileEndAccessTime: OffsetTime? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_PROJECT_ID)
	var preferenceSelectedProfileProjectId: UUID? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_PROJECT_NAME)
	var preferenceSelectedProfileProjectName: String? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_PROJECT_START_DATE)
	var preferenceSelectedProfileProjectStartDate: LocalDate? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_PROJECT_START_TIME)
	var preferenceSelectedProfileProjectStartTime: OffsetTime? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_PROJECT_END_DATE)
	var preferenceSelectedProfileProjectEndDate: LocalDate? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_PROJECT_END_TIME)
	var preferenceSelectedProfileProjectEndTime: OffsetTime? = null,
	@ReadOnlyProperty
	@Column(PREFERENCE_SELECTED_PROFILE_PROJECT_OPTIONS)
	var preferenceSelectedProfileProjectOptions: List<ProjectOptionEnum>? = null,
): UserEntity()

