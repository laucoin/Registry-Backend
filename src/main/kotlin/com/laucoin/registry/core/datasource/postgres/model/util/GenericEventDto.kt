package com.laucoin.registry.core.datasource.postgres.model.util

import com.laucoin.registry.core.model.event.EventOptionEnum
import com.laucoin.registry.core.model.util.GenericEventModel
import com.laucoin.registry.core.model.util.GenericModel
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

open class GenericEventDto(
    @Column(eventIdField)
    var eventId: UUID? = null,
    @ReadOnlyProperty
    @Column(linkedEventNameField)
    var eventName: String? = null,
    @ReadOnlyProperty
    @Column(linkedEventAddressIdField)
    var eventAddressId: UUID? = null,
    @ReadOnlyProperty
    @Column(linkedEventAddressNumberField)
    var eventAddressNumber: String? = null,
    @ReadOnlyProperty
    @Column(linkedEventAddressStreetField)
    var eventAddressStreet: String? = null,
    @ReadOnlyProperty
    @Column(linkedEventAddressComplementaryInformationField)
    var eventAddressComplementaryInformation: String? = null,
    @ReadOnlyProperty
    @Column(linkedEventAddressZipCodeField)
    var eventAddressZipCode: String? = null,
    @ReadOnlyProperty
    @Column(linkedEventAddressCityField)
    var eventAddressCity: String? = null,
    @ReadOnlyProperty
    @Column(linkedEventAddressCountryField)
    var eventAddressCountry: String? = null,
    @ReadOnlyProperty
    @Column(linkedEventOptionsField)
    var eventOptions: List<EventOptionEnum> = emptyList(),
    @ReadOnlyProperty
    @Column(linkedEventStartTimeField)
    var eventStartTime: LocalDateTime? = null,
    @ReadOnlyProperty
    @Column(linkedEventEndTimeField)
    var eventEndTime: LocalDateTime? = null,
    @ReadOnlyProperty
    @Column(linkedEventVisibleField)
    var eventVisible: Boolean? = null,
): GenericDto() {
    override fun <T: GenericModel> populateGenericDto(element: T) {
        super.populateGenericDto(element)

        if (element is GenericEventModel) {
            eventId = element.eventId
        }
    }

    override fun <T: GenericModel> populateGenericModel(element: T): T {
        super.populateGenericModel(element)

        if (element is GenericEventModel) {
            element.eventId = eventId
            element.eventName = eventName
            element.eventAddressId = eventAddressId
            element.eventAddressNumber = eventAddressNumber
            element.eventAddressStreet = eventAddressStreet
            element.eventAddressComplementaryInformation = eventAddressComplementaryInformation
            element.eventAddressZipCode = eventAddressZipCode
            element.eventAddressCity = eventAddressCity
            element.eventAddressCountry = eventAddressCountry
            element.eventOptions = eventOptions
            element.eventStartTime = eventStartTime
            element.eventEndTime = eventEndTime
            element.eventVisible = eventVisible
        }

        return element
    }
}
