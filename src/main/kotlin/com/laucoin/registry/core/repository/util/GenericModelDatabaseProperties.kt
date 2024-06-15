package com.laucoin.registry.core.repository.util

import com.laucoin.registry.core.model.event.eventAddressCityField
import com.laucoin.registry.core.model.event.eventAddressComplementaryInformationField
import com.laucoin.registry.core.model.event.eventAddressCountryField
import com.laucoin.registry.core.model.event.eventAddressIdField
import com.laucoin.registry.core.model.event.eventAddressNumberField
import com.laucoin.registry.core.model.event.eventAddressStreetField
import com.laucoin.registry.core.model.event.eventAddressZipCodeField
import com.laucoin.registry.core.model.event.eventEndTimeField
import com.laucoin.registry.core.model.event.eventNameField
import com.laucoin.registry.core.model.event.eventOptionsField
import com.laucoin.registry.core.model.event.eventStartTimeField
import com.laucoin.registry.core.model.user.userEmailField
import com.laucoin.registry.core.model.user.userFirstNameField
import com.laucoin.registry.core.model.user.userLastNameField

const val user = "user_"

const val idField = "id"
const val visibleField = "visible"
const val eventIdField = "event_id"

const val createPrefix = "create_"
const val createDateField = createPrefix + "date"
const val createUserIdField = createPrefix + user + idField
const val createUserFirstNameField = createPrefix + user + userFirstNameField
const val createUserLastNameField = createPrefix + user + userLastNameField
const val createUserEmailField = createPrefix + user + userEmailField
const val createUserVisibleField = createPrefix + user + visibleField

const val editPrefix = "edit_"
const val editDateField = editPrefix + "date"
const val editUserIdField = editPrefix + user + idField
const val editUserFirstNameField = editPrefix + user + userFirstNameField
const val editUserLastNameField = editPrefix + user + userLastNameField
const val editUserEmailField = editPrefix + user + userEmailField
const val editUserVisibleField = editPrefix + user + visibleField

const val linkedEventPrefix = "event_"
const val linkedEventNameField = linkedEventPrefix + eventNameField
const val linkedEventAddressIdField = linkedEventPrefix + eventAddressIdField
const val linkedEventAddressNumberField = linkedEventPrefix + eventAddressNumberField
const val linkedEventAddressStreetField = linkedEventPrefix + eventAddressStreetField
const val linkedEventAddressComplementaryInformationField = linkedEventPrefix + eventAddressComplementaryInformationField
const val linkedEventAddressZipCodeField = linkedEventPrefix + eventAddressZipCodeField
const val linkedEventAddressCityField = linkedEventPrefix + eventAddressCityField
const val linkedEventAddressCountryField = linkedEventPrefix + eventAddressCountryField
const val linkedEventOptionsField = linkedEventPrefix + eventOptionsField
const val linkedEventStartTimeField = linkedEventPrefix + eventStartTimeField
const val linkedEventEndTimeField = linkedEventPrefix + eventEndTimeField
const val linkedEventVisibleField = linkedEventPrefix + visibleField
