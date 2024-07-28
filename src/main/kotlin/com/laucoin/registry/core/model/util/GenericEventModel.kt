package com.laucoin.registry.core.model.util

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
import com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY
import com.laucoin.registry.core.model.event.AddressModel
import com.laucoin.registry.core.model.event.EnrichedEventModel
import com.laucoin.registry.core.model.event.EventOptionEnum
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

abstract class GenericEventModel(
    @field:NotNull
    @JsonProperty(access = WRITE_ONLY)
    var eventId: UUID? = null,
    @JsonIgnore
    var eventName: String? = null,
    @JsonIgnore
    var eventAddressId: UUID? = null,
    @JsonIgnore
    var eventAddressNumber: String? = null,
    @JsonIgnore
    var eventAddressStreet: String? = null,
    @JsonIgnore
    var eventAddressComplementaryInformation: String? = null,
    @JsonIgnore
    var eventAddressZipCode: String? = null,
    @JsonIgnore
    var eventAddressCity: String? = null,
    @JsonIgnore
    var eventAddressCountry: String? = null,
    @JsonIgnore
    var eventOptions: List<EventOptionEnum> = emptyList(),
    @JsonIgnore
    var eventStartTime: LocalDateTime? = null,
    @JsonIgnore
    var eventEndTime: LocalDateTime? = null,
    @JsonIgnore
    var eventVisible: Boolean? = null,
): GenericModel() {
    @JsonProperty(access = READ_ONLY)
    fun event(): EnrichedEventModel {
        val address = AddressModel(
            eventAddressNumber,
            eventAddressStreet,
            eventAddressComplementaryInformation,
            eventAddressZipCode,
            eventAddressCity,
            eventAddressCountry
        ).apply { id = eventAddressId }

        return EnrichedEventModel(address).apply {
            id = eventId
            name = eventName
            addressId = eventAddressId
            options = eventOptions
            startTime = eventStartTime
            endTime = eventEndTime
            visible = eventVisible ?: visible
        }
    }
}
