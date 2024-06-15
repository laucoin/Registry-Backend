package com.laucoin.registry.core.model.user

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
import com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY
import com.laucoin.registry.core.config.GsonConfig
import com.laucoin.registry.core.model.event.EventOptionEnum
import com.laucoin.registry.core.model.profile.UserProfileModel
import com.laucoin.registry.core.model.util.GenericModel
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(userTable)
data class UserModel(
    @Column(oidcIdField)
    @JsonProperty(access = READ_ONLY)
    var oidcId: UUID? = null,

    @Column(userFirstNameField)
    @JsonProperty(access = READ_ONLY)
    var firstName: String? = null,

    @Column(userLastNameField)
    @JsonProperty(access = READ_ONLY)
    var lastName: String? = null,

    @JsonProperty(access = READ_ONLY)
    var email: String? = null,

    val role: String? = null,

    @JsonProperty(access = WRITE_ONLY)
    @Column(userDefaultProfileIdField)
    var defaultProfileId: UUID? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileRoleField)
    var defaultProfileRole: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileAcceptedField)
    var defaultProfileAccepted: Boolean = false,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileStartAccessField)
    var defaultProfileStartAccess: LocalDateTime? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEndAccessField)
    var defaultProfileEndAccess: LocalDateTime? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileVisibleField)
    var defaultProfileVisible: Boolean? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventIdField)
    var defaultProfileEventId: UUID? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventNameField)
    val defaultProfileEventName: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressIdField)
    val defaultProfileEventAddressId: UUID? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressNumberField)
    val defaultProfileEventAddressNumber: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressStreetField)
    val defaultProfileEventAddressStreet: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressComplementaryInformationField)
    val defaultProfileEventAddressComplementaryInformation: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressZipCodeField)
    val defaultProfileEventAddressZipCode: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressCityField)
    val defaultProfileEventAddressCity: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressCountryField)
    val defaultProfileEventAddressCountry: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventOptionsField)
    val defaultProfileEventOptions: List<EventOptionEnum> = emptyList(),
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventStartTimeField)
    val defaultProfileEventStartTime: LocalDateTime? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(userDefaultProfileEventEndTimeField)
    val defaultProfileEventEndTime: LocalDateTime? = null,
): GenericModel() {
    @ReadOnlyProperty
    @JsonProperty
    fun defaultProfile(): UserProfileModel? {
        if (Objects.isNull(defaultProfileId)) return null
        return UserProfileModel(
            userId = id,
            role = defaultProfileRole,
            accepted = defaultProfileAccepted,
            startAccess = defaultProfileStartAccess,
            endAccess = defaultProfileEndAccess,
        ).apply {
            id = defaultProfileId
            visible = defaultProfileVisible ?: visible
            eventId = defaultProfileEventId
            eventName = defaultProfileEventName
            eventAddressId = defaultProfileEventAddressId
            eventAddressNumber = defaultProfileEventAddressNumber
            eventAddressStreet = defaultProfileEventAddressStreet
            eventAddressComplementaryInformation = defaultProfileEventAddressComplementaryInformation
            eventAddressZipCode = defaultProfileEventAddressZipCode
            eventAddressCity = defaultProfileEventAddressCity
            eventAddressCountry = defaultProfileEventAddressCountry
            eventOptions = defaultProfileEventOptions
            eventStartTime = defaultProfileEventStartTime
            eventEndTime = defaultProfileEventEndTime
        }
    }

    override fun toString(): String {
        return GsonConfig().gson().toJson(this)
    }

    fun personalDataChanged(
        newEmail: String,
        newFirstName: String?,
        newLastName: String?
    ): Boolean = email !== newEmail || firstName !== newFirstName || lastName !== newLastName

    fun defaultProfileDisable(): Boolean = Objects.nonNull(defaultProfileVisible) && defaultProfileVisible == false
}
