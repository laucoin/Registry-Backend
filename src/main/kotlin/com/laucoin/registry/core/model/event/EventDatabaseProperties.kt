package com.laucoin.registry.core.model.event

import com.laucoin.registry.core.repository.util.idField

const val eventTable = "tb_event"
const val address = "address_"

const val eventNameField = "name"
const val eventAddressIdField = address + idField
const val eventAddressNumberField = address + addressNumberField
const val eventAddressStreetField = address + addressStreetField
const val eventAddressComplementaryInformationField = address + addressComplementaryInformationField
const val eventAddressZipCodeField = address + addressZipCodeField
const val eventAddressCityField = address + addressCityField
const val eventAddressCountryField = address + addressCountryField
const val eventOptionsField = "options"
const val eventStartTimeField = "start_time"
const val eventEndTimeField = "end_time"
