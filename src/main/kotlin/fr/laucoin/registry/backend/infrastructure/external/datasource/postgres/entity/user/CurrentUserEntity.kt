package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
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
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.PREFERENCE_ID
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

data class CurrentUserEntity(
    @ReadOnlyProperty
    @Column(PREFERENCE_ID)
    var preferenceId: UUID? = null,
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
    var preferenceSelectedProfileStartAccessTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE)
    var preferenceSelectedProfileEndAccessDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME)
    var preferenceSelectedProfileEndAccessTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_ID)
    var preferenceSelectedProfileEventId: UUID? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_NAME)
    var preferenceSelectedProfileEventName: String? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_START_DATE)
    var preferenceSelectedProfileEventStartDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_START_TIME)
    var preferenceSelectedProfileEventStartTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_END_DATE)
    var preferenceSelectedProfileEventEndDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_END_TIME)
    var preferenceSelectedProfileEventEndTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(PREFERENCE_SELECTED_PROFILE_EVENT_OPTIONS)
    var preferenceSelectedProfileEventOptions: List<EventOptionEnum>? = null,
): UserEntity()

