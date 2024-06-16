package com.laucoin.registry.core.datasource.postgres.model.util

const val user = "user_"

const val idField = "id"
const val visibleField = "visible"
const val eventIdField = "event_id"
const val userIdField = user + idField

const val createPrefix = "create_"
const val createDateField = createPrefix + "date"
const val createUserIdField = createPrefix + userIdField
const val createUserFirstNameField = createPrefix + user + userFirstNameField
const val createUserLastNameField = createPrefix + user + userLastNameField
const val createUserEmailField = createPrefix + user + userEmailField
const val createUserVisibleField = createPrefix + user + visibleField

const val editPrefix = "edit_"
const val editDateField = editPrefix + "date"
const val editUserIdField = editPrefix + userIdField
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
