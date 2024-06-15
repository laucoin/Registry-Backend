package com.laucoin.registry.core.model.event

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY
import com.laucoin.registry.core.model.util.GenericModel
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(eventTable)
data class EventModel(
    val name: String? = null,
    @JsonProperty(access = WRITE_ONLY)
    @Column(eventAddressIdField)
    val addressId: UUID? = null,
    val options: List<EventOptionEnum> = emptyList(),
    @Column(eventStartTimeField)
    val startTime: LocalDateTime? = null,
    @Column(eventEndTimeField)
    val endTime: LocalDateTime? = null,

    @JsonIgnore
    @ReadOnlyProperty
    @Column(eventAddressNumberField)
    val addressNumber: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(eventAddressStreetField)
    val addressStreet: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(eventAddressComplementaryInformationField)
    val addressComplementaryInformation: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(eventAddressZipCodeField)
    val addressZipCode: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(eventAddressCityField)
    val addressCity: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(eventAddressCountryField)
    val addressCountry: String? = null,
): GenericModel() {
    @ReadOnlyProperty
    @JsonProperty
    fun address(): AddressModel? {
        if (Objects.isNull(addressId)) return null
        return AddressModel(
            number = addressNumber,
            street = addressStreet,
            complementaryInformation = addressComplementaryInformation,
            zipCode = addressZipCode,
            city = addressCity,
            country = addressCountry
        ).apply { id = addressId }
    }
}
