package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_USER_PURGED
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(EVENT_PROFILE_TABLE)
data class EventProfileEntity(
    @Column(EVENT_PROFILE_USER_ID)
    var userId: UUID? = null,
    @ReadOnlyProperty
    @Column(EVENT_PROFILE_USER_FIRST_NAME)
    var userFirstName: String? = null,
    @ReadOnlyProperty
    @Column(EVENT_PROFILE_USER_LAST_NAME)
    var userLastName: String? = null,
    @ReadOnlyProperty
    @Column(EVENT_PROFILE_USER_EMAIL)
    var userEmail: String? = null,
    @ReadOnlyProperty
    @Column(EVENT_PROFILE_USER_LAST_LOGIN)
    var userLastLogin: ZonedDateTime? = null,
    @ReadOnlyProperty
    @Column(EVENT_PROFILE_USER_PURGED)
    var userPurged: Boolean? = null,

    @Column(EVENT_PROFILE_ROLE)
    var role: String? = null,
    @Column(EVENT_PROFILE_STATUS)
    var status: ProfileStatusEnum? = null,
    @Column(EVENT_PROFILE_START_ACCESS_DATE)
    var startAccessDate: LocalDate? = null,
    @Column(EVENT_PROFILE_START_ACCESS_TIME)
    var startAccessTime: LocalTime? = null,
    @Column(EVENT_PROFILE_END_ACCESS_DATE)
    var endAccessDate: LocalDate? = null,
    @Column(EVENT_PROFILE_END_ACCESS_TIME)
    var endAccessTime: LocalTime? = null,
): GenericEventEntity()
