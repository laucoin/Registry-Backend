package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_START_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_PROJECT_START_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_USER_ID
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(PREFERENCE_TABLE)
data class PreferencesEntity(
    @Column(PREFERENCE_USER_ID)
    var userId: UUID? = null,
    @Column(PREFERENCE_SELECTED_PROFILE_ID)
    var selectedProfileId: UUID? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_PROJECT_ID)
    var selectedProfileProjectId: UUID? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_PROJECT_NAME)
    var selectedProfileProjectName: String? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_PROJECT_START_DATE)
    var selectedProfileProjectStartDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_PROJECT_START_TIME)
    var selectedProfileProjectStartTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_PROJECT_END_DATE)
    var selectedProfileProjectEndDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_PROJECT_END_TIME)
    var selectedProfileProjectEndTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_PROJECT_OPTIONS)
    var selectedProfileProjectOptions: List<ProjectOptionEnum>? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_ROLE)
    var selectedProfileRole: String? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_STATUS)
    var selectedProfileStatus: ProfileStatusEnum? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE)
    var selectedProfileStartAccessDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME)
    var selectedProfileStartAccessTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE)
    var selectedProfileEndAccessDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME)
    var selectedProfileEndAccessTime: LocalTime? = null,
): GenericEntity()

