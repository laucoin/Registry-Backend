package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_START_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferenceFields.PREFERENCE_SELECTED_PROFILE_EVENT_START_TIME
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
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_ID)
    var selectedProfileEventId: UUID? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_NAME)
    var selectedProfileEventName: String? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_START_DATE)
    var selectedProfileEventStartDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_START_TIME)
    var selectedProfileEventStartTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_END_DATE)
    var selectedProfileEventEndDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_END_TIME)
    var selectedProfileEventEndTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_OPTIONS)
    var selectedProfileEventOptions: List<EventOptionEnum>? = null,
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

