package com.laucoin.registry.core.datasource.postgres.model

import com.laucoin.registry.core.datasource.postgres.model.util.GenericDto
import com.laucoin.registry.core.datasource.postgres.model.util.eventAddressCityField
import com.laucoin.registry.core.datasource.postgres.model.util.eventAddressComplementaryInformationField
import com.laucoin.registry.core.datasource.postgres.model.util.eventAddressCountryField
import com.laucoin.registry.core.datasource.postgres.model.util.eventAddressIdField
import com.laucoin.registry.core.datasource.postgres.model.util.eventAddressNumberField
import com.laucoin.registry.core.datasource.postgres.model.util.eventAddressStreetField
import com.laucoin.registry.core.datasource.postgres.model.util.eventAddressZipCodeField
import com.laucoin.registry.core.datasource.postgres.model.util.eventEndTimeField
import com.laucoin.registry.core.datasource.postgres.model.util.eventStartTimeField
import com.laucoin.registry.core.datasource.postgres.model.util.eventTable
import com.laucoin.registry.core.model.event.AddressModel
import com.laucoin.registry.core.model.event.EnrichedEventModel
import com.laucoin.registry.core.model.event.EventModel
import com.laucoin.registry.core.model.event.EventOptionEnum
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(eventTable)
data class EventDto(
    var name: String? = null,
    @Column(eventAddressIdField)
    var addressId: UUID? = null,
    var options: List<EventOptionEnum> = emptyList(),
    @Column(eventStartTimeField)
    var startTime: LocalDateTime? = null,
    @Column(eventEndTimeField)
    var endTime: LocalDateTime? = null,
    @ReadOnlyProperty
    @Column(eventAddressNumberField)
    var addressNumber: String? = null,
    @ReadOnlyProperty
    @Column(eventAddressStreetField)
    var addressStreet: String? = null,
    @ReadOnlyProperty
    @Column(eventAddressComplementaryInformationField)
    var addressComplementaryInformation: String? = null,
    @ReadOnlyProperty
    @Column(eventAddressZipCodeField)
    var addressZipCode: String? = null,
    @ReadOnlyProperty
    @Column(eventAddressCityField)
    var addressCity: String? = null,
    @ReadOnlyProperty
    @Column(eventAddressCountryField)
    var addressCountry: String? = null,
): GenericDto() {
    constructor(event: EventModel): this() {
        name = event.name
        addressId = event.addressId
        options = event.options
        startTime = event.startTime
        endTime = event.endTime
        populateGenericDto(event)
    }

    fun toModel(): EnrichedEventModel {
        val event = EnrichedEventModel()
        event.let {
            it.name = name
            it.options = options
            it.startTime = startTime
            it.endTime = endTime
            it.addressId = addressId

            if (Objects.nonNull(addressId)) {
                it.address = AddressModel()
                it.address !!.id = addressId
                it.address !!.number = addressNumber
                it.address !!.street = addressStreet
                it.address !!.complementaryInformation = addressComplementaryInformation
                it.address !!.zipCode = addressZipCode
                it.address !!.city = addressCity
                it.address !!.country = addressCountry
            }

            populateGenericModel(element = it)
        }
        return event
    }
}
