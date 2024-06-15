package com.laucoin.registry.core.model.util

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY
import com.laucoin.registry.core.model.event.EventModel
import com.laucoin.registry.core.model.event.EventOptionEnum
import com.laucoin.registry.core.repository.util.eventIdField
import com.laucoin.registry.core.repository.util.linkedEventAddressCityField
import com.laucoin.registry.core.repository.util.linkedEventAddressComplementaryInformationField
import com.laucoin.registry.core.repository.util.linkedEventAddressCountryField
import com.laucoin.registry.core.repository.util.linkedEventAddressIdField
import com.laucoin.registry.core.repository.util.linkedEventAddressNumberField
import com.laucoin.registry.core.repository.util.linkedEventAddressStreetField
import com.laucoin.registry.core.repository.util.linkedEventAddressZipCodeField
import com.laucoin.registry.core.repository.util.linkedEventEndTimeField
import com.laucoin.registry.core.repository.util.linkedEventNameField
import com.laucoin.registry.core.repository.util.linkedEventOptionsField
import com.laucoin.registry.core.repository.util.linkedEventStartTimeField
import com.laucoin.registry.core.repository.util.linkedEventVisibleField
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

open class GenericEventModel(
    @Column(eventIdField)
    @JsonProperty(access = WRITE_ONLY)
    var eventId: UUID? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventNameField)
    var eventName: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventAddressIdField)
    var eventAddressId: UUID? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventAddressNumberField)
    var eventAddressNumber: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventAddressStreetField)
    var eventAddressStreet: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventAddressComplementaryInformationField)
    var eventAddressComplementaryInformation: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventAddressZipCodeField)
    var eventAddressZipCode: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventAddressCityField)
    var eventAddressCity: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventAddressCountryField)
    var eventAddressCountry: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventOptionsField)
    var eventOptions: List<EventOptionEnum> = emptyList(),
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventStartTimeField)
    var eventStartTime: LocalDateTime? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventEndTimeField)
    var eventEndTime: LocalDateTime? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(linkedEventVisibleField)
    var eventVisible: Boolean? = null,
): GenericModel() {
    @ReadOnlyProperty
    @JsonProperty
    fun event(): EventModel = EventModel(
        name = eventName,
        addressId = eventAddressId,
        options = eventOptions,
        startTime = eventStartTime,
        endTime = eventEndTime,
        addressNumber = eventAddressNumber,
        addressStreet = eventAddressStreet,
        addressComplementaryInformation = eventAddressComplementaryInformation,
        addressZipCode = eventAddressZipCode,
        addressCity = eventAddressCity,
        addressCountry = eventAddressCountry
    ).apply {
        id = eventId
        visible = eventVisible ?: visible
    }
}
