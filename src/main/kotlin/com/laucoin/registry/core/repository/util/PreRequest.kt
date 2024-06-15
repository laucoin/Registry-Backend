package com.laucoin.registry.core.repository.util

import com.laucoin.registry.core.model.event.addressCityField
import com.laucoin.registry.core.model.event.addressComplementaryInformationField
import com.laucoin.registry.core.model.event.addressCountryField
import com.laucoin.registry.core.model.event.addressNumberField
import com.laucoin.registry.core.model.event.addressStreetField
import com.laucoin.registry.core.model.event.addressTable
import com.laucoin.registry.core.model.event.addressZipCodeField
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
import com.laucoin.registry.core.model.event.eventTable
import com.laucoin.registry.core.model.profile.profileAcceptedField
import com.laucoin.registry.core.model.profile.profileEndAccessField
import com.laucoin.registry.core.model.profile.profileRoleField
import com.laucoin.registry.core.model.profile.profileStartAccessField
import com.laucoin.registry.core.model.profile.profileTable
import com.laucoin.registry.core.model.user.userDefaultProfileAcceptedField
import com.laucoin.registry.core.model.user.userDefaultProfileEndAccessField
import com.laucoin.registry.core.model.user.userDefaultProfileEventAddressCityField
import com.laucoin.registry.core.model.user.userDefaultProfileEventAddressComplementaryInformationField
import com.laucoin.registry.core.model.user.userDefaultProfileEventAddressCountryField
import com.laucoin.registry.core.model.user.userDefaultProfileEventAddressIdField
import com.laucoin.registry.core.model.user.userDefaultProfileEventAddressNumberField
import com.laucoin.registry.core.model.user.userDefaultProfileEventAddressStreetField
import com.laucoin.registry.core.model.user.userDefaultProfileEventAddressZipCodeField
import com.laucoin.registry.core.model.user.userDefaultProfileEventEndTimeField
import com.laucoin.registry.core.model.user.userDefaultProfileEventIdField
import com.laucoin.registry.core.model.user.userDefaultProfileEventNameField
import com.laucoin.registry.core.model.user.userDefaultProfileEventOptionsField
import com.laucoin.registry.core.model.user.userDefaultProfileEventStartTimeField
import com.laucoin.registry.core.model.user.userDefaultProfileIdField
import com.laucoin.registry.core.model.user.userDefaultProfileRoleField
import com.laucoin.registry.core.model.user.userDefaultProfileStartAccessField
import com.laucoin.registry.core.model.user.userDefaultProfileVisibleField
import com.laucoin.registry.core.model.user.userEmailField
import com.laucoin.registry.core.model.user.userFirstNameField
import com.laucoin.registry.core.model.user.userLastNameField
import com.laucoin.registry.core.model.user.userTable

const val creatorTable = "create_tb"
const val selectCreationInformation =
    "$creatorTable.$userFirstNameField AS $createUserFirstNameField, " +
    "$creatorTable.$userLastNameField AS $createUserLastNameField, " +
    "$creatorTable.$userEmailField AS $createUserEmailField, " +
    "$creatorTable.$visibleField AS $createUserVisibleField"
const val creatorJoin = "LEFT JOIN $userTable $creatorTable ON t.$createUserIdField = $creatorTable.$idField"

const val editorTable = "edit_tb"
const val selectEditionInformation =
    "$editorTable.$userFirstNameField AS $editUserFirstNameField, " +
    "$editorTable.$userLastNameField AS $editUserLastNameField, " +
    "$editorTable.$userEmailField AS $editUserEmailField, " +
    "$editorTable.$visibleField AS $editUserVisibleField"
const val editorJoin = "LEFT JOIN $userTable $editorTable ON t.$editUserIdField = $editorTable.$idField"

const val linkedEventTable = "linked_event_tb"
const val linkedEventAddressTable = "linked_event_address_tb"

const val selectEvent =
    "$linkedEventTable.$eventNameField AS $linkedEventNameField, " +
    "$linkedEventTable.$eventAddressIdField AS $linkedEventAddressIdField, " +
    "$linkedEventAddressTable.$addressNumberField AS $linkedEventAddressNumberField, " +
    "$linkedEventAddressTable.$addressStreetField AS $linkedEventAddressStreetField, " +
    "$linkedEventAddressTable.$addressComplementaryInformationField AS $linkedEventAddressComplementaryInformationField, " +
    "$linkedEventAddressTable.$addressZipCodeField AS $linkedEventAddressZipCodeField, " +
    "$linkedEventAddressTable.$addressCityField AS $linkedEventAddressCityField, " +
    "$linkedEventAddressTable.$addressCountryField AS $linkedEventAddressCountryField, " +
    "$linkedEventTable.$eventOptionsField AS $linkedEventOptionsField, " +
    "$linkedEventTable.$eventStartTimeField AS $linkedEventStartTimeField, " +
    "$linkedEventTable.$eventEndTimeField AS $linkedEventEndTimeField, " +
    "$linkedEventTable.$visibleField AS $linkedEventVisibleField"
const val eventJoin = "INNER JOIN $eventTable $linkedEventTable ON t.$eventIdField = $linkedEventTable.$idField " +
                      "LEFT JOIN $addressTable $linkedEventAddressTable ON $linkedEventTable.$eventAddressIdField = $linkedEventAddressTable.$idField"

const val defaultProfileTable = "default_profile_tb"
const val selectDefaultProfile =
    "$defaultProfileTable.$profileRoleField AS $userDefaultProfileRoleField, " +
    "$defaultProfileTable.$profileAcceptedField AS $userDefaultProfileAcceptedField, " +
    "$defaultProfileTable.$profileStartAccessField AS $userDefaultProfileStartAccessField, " +
    "$defaultProfileTable.$profileEndAccessField AS $userDefaultProfileEndAccessField, " +
    "$defaultProfileTable.$visibleField AS $userDefaultProfileVisibleField, " +
    "$defaultProfileTable.$eventIdField AS $userDefaultProfileEventIdField, " +
    "$linkedEventTable.$eventNameField AS $userDefaultProfileEventNameField, " +
    "$linkedEventTable.$eventAddressIdField AS $userDefaultProfileEventAddressIdField, " +
    "$linkedEventAddressTable.$addressNumberField AS $userDefaultProfileEventAddressNumberField, " +
    "$linkedEventAddressTable.$addressStreetField AS $userDefaultProfileEventAddressStreetField, " +
    "$linkedEventAddressTable.$addressComplementaryInformationField AS $userDefaultProfileEventAddressComplementaryInformationField, " +
    "$linkedEventAddressTable.$addressZipCodeField AS $userDefaultProfileEventAddressZipCodeField, " +
    "$linkedEventAddressTable.$addressCityField AS $userDefaultProfileEventAddressCityField, " +
    "$linkedEventAddressTable.$addressCountryField AS $userDefaultProfileEventAddressCountryField, " +
    "$linkedEventTable.$eventOptionsField AS $userDefaultProfileEventOptionsField, " +
    "$linkedEventTable.$eventStartTimeField AS $userDefaultProfileEventStartTimeField, " +
    "$linkedEventTable.$eventEndTimeField AS $userDefaultProfileEventEndTimeField"
const val defaultProfileJoin =
    "LEFT JOIN $profileTable $defaultProfileTable ON t.$userDefaultProfileIdField = $defaultProfileTable.$idField " +
    "LEFT JOIN $eventTable $linkedEventTable ON $defaultProfileTable.$eventIdField = $linkedEventTable.$idField " +
    "LEFT JOIN $addressTable $linkedEventAddressTable ON $linkedEventTable.$eventAddressIdField = $linkedEventAddressTable.$idField"

const val eventAddressTable = "event_address_tb"
const val selectAddress =
    "$eventAddressTable.$addressNumberField AS $eventAddressNumberField, " +
    "$eventAddressTable.$addressStreetField AS $eventAddressStreetField, " +
    "$eventAddressTable.$addressComplementaryInformationField AS $eventAddressComplementaryInformationField, " +
    "$eventAddressTable.$addressZipCodeField AS $eventAddressZipCodeField, " +
    "$eventAddressTable.$addressCityField AS $eventAddressCityField, " +
    "$eventAddressTable.$addressCountryField AS $eventAddressCountryField"
const val addressJoin = "LEFT JOIN $addressTable $eventAddressTable ON t.$eventAddressIdField = $eventAddressTable.$idField"

const val onlyVisibleClause = "(:onlyVisible IS FALSE OR t.$visibleField IS TRUE)"
