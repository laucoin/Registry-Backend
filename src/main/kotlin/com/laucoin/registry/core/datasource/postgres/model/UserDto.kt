package com.laucoin.registry.core.datasource.postgres.model

import com.laucoin.registry.core.datasource.postgres.model.util.GenericDto
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileAcceptedField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEndAccessField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventAddressCityField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventAddressComplementaryInformationField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventAddressCountryField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventAddressIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventAddressNumberField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventAddressStreetField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventAddressZipCodeField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventEndTimeField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventOptionsField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileEventStartTimeField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileRoleField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileStartAccessField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileVisibleField
import com.laucoin.registry.core.datasource.postgres.model.util.userFirstNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userLastNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userOidcIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userTable
import com.laucoin.registry.core.model.event.EventOptionEnum
import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(userTable)
data class UserDto(
    @Column(userOidcIdField)
    var oidcId: UUID? = null,
    @Column(userFirstNameField)
    var firstName: String? = null,
    @Column(userLastNameField)
    var lastName: String? = null,
    var email: String? = null,
    var role: String? = null,
    @Column(userDefaultProfileIdField)
    var defaultProfileId: UUID? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileRoleField)
    var defaultProfileRole: String? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileAcceptedField)
    var defaultProfileAccepted: Boolean = false,
    @ReadOnlyProperty
    @Column(userDefaultProfileStartAccessField)
    var defaultProfileStartAccess: LocalDateTime? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEndAccessField)
    var defaultProfileEndAccess: LocalDateTime? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileVisibleField)
    var defaultProfileVisible: Boolean? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventIdField)
    var defaultProfileEventId: UUID? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventNameField)
    val defaultProfileEventName: String? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressIdField)
    val defaultProfileEventAddressId: UUID? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressNumberField)
    val defaultProfileEventAddressNumber: String? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressStreetField)
    val defaultProfileEventAddressStreet: String? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressComplementaryInformationField)
    val defaultProfileEventAddressComplementaryInformation: String? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressZipCodeField)
    val defaultProfileEventAddressZipCode: String? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressCityField)
    val defaultProfileEventAddressCity: String? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventAddressCountryField)
    val defaultProfileEventAddressCountry: String? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventOptionsField)
    val defaultProfileEventOptions: List<EventOptionEnum> = emptyList(),
    @ReadOnlyProperty
    @Column(userDefaultProfileEventStartTimeField)
    val defaultProfileEventStartTime: LocalDateTime? = null,
    @ReadOnlyProperty
    @Column(userDefaultProfileEventEndTimeField)
    val defaultProfileEventEndTime: LocalDateTime? = null,
): GenericDto() {
    constructor(user: UserModel): this() {
        oidcId = user.oidcId
        firstName = user.firstName
        lastName = user.lastName
        email = user.email
        role = user.role
        defaultProfileId = user.defaultProfileId
        populateGenericDto(user)
    }

    fun toModel(): EnrichedUserModel {
        val user = EnrichedUserModel()
        user.let {
            it.oidcId = oidcId
            it.firstName = firstName
            it.lastName = lastName
            it.email = email
            it.role = role
            it.defaultProfileId = defaultProfileId

            if (Objects.nonNull(defaultProfileId)) {
                it.defaultProfile = ProfileModel()
                it.defaultProfile !!.let { profile ->
                    profile.id = defaultProfileId
                    profile.role = defaultProfileRole
                    profile.accepted = defaultProfileAccepted
                    profile.startAccess = defaultProfileStartAccess
                    profile.endAccess = defaultProfileEndAccess
                    profile.visible = defaultProfileVisible ?: visible
                    profile.eventId = defaultProfileEventId
                    profile.eventName = defaultProfileEventName
                    profile.eventAddressId = defaultProfileEventAddressId
                    profile.eventAddressNumber = defaultProfileEventAddressNumber
                    profile.eventAddressStreet = defaultProfileEventAddressStreet
                    profile.eventAddressComplementaryInformation = defaultProfileEventAddressComplementaryInformation
                    profile.eventAddressZipCode = defaultProfileEventAddressZipCode
                    profile.eventAddressCity = defaultProfileEventAddressCity
                    profile.eventAddressCountry = defaultProfileEventAddressCountry
                    profile.eventOptions = defaultProfileEventOptions
                    profile.eventStartTime = defaultProfileEventStartTime
                    profile.eventEndTime = defaultProfileEventEndTime

                }
            }

            populateGenericModel(element = it)
        }
        return user
    }
}
