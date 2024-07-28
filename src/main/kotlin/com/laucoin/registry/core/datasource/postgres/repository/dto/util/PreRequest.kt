package com.laucoin.registry.core.datasource.postgres.repository.dto.util

import com.laucoin.registry.core.datasource.postgres.model.util.addressCityField
import com.laucoin.registry.core.datasource.postgres.model.util.addressComplementaryInformationField
import com.laucoin.registry.core.datasource.postgres.model.util.addressCountryField
import com.laucoin.registry.core.datasource.postgres.model.util.addressNumberField
import com.laucoin.registry.core.datasource.postgres.model.util.addressStreetField
import com.laucoin.registry.core.datasource.postgres.model.util.addressTable
import com.laucoin.registry.core.datasource.postgres.model.util.addressZipCodeField
import com.laucoin.registry.core.datasource.postgres.model.util.createUserEmailField
import com.laucoin.registry.core.datasource.postgres.model.util.createUserFirstNameField
import com.laucoin.registry.core.datasource.postgres.model.util.createUserIdField
import com.laucoin.registry.core.datasource.postgres.model.util.createUserLastNameField
import com.laucoin.registry.core.datasource.postgres.model.util.createUserVisibleField
import com.laucoin.registry.core.datasource.postgres.model.util.editUserEmailField
import com.laucoin.registry.core.datasource.postgres.model.util.editUserFirstNameField
import com.laucoin.registry.core.datasource.postgres.model.util.editUserIdField
import com.laucoin.registry.core.datasource.postgres.model.util.editUserLastNameField
import com.laucoin.registry.core.datasource.postgres.model.util.editUserVisibleField
import com.laucoin.registry.core.datasource.postgres.model.util.eventAddressIdField
import com.laucoin.registry.core.datasource.postgres.model.util.eventEndTimeField
import com.laucoin.registry.core.datasource.postgres.model.util.eventIdField
import com.laucoin.registry.core.datasource.postgres.model.util.eventNameField
import com.laucoin.registry.core.datasource.postgres.model.util.eventOptionsField
import com.laucoin.registry.core.datasource.postgres.model.util.eventStartTimeField
import com.laucoin.registry.core.datasource.postgres.model.util.eventTable
import com.laucoin.registry.core.datasource.postgres.model.util.idField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventAddressCityField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventAddressComplementaryInformationField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventAddressCountryField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventAddressIdField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventAddressNumberField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventAddressStreetField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventAddressZipCodeField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventEndTimeField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventNameField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventOptionsField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventStartTimeField
import com.laucoin.registry.core.datasource.postgres.model.util.linkedEventVisibleField
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
import com.laucoin.registry.core.datasource.postgres.model.util.userEmailField
import com.laucoin.registry.core.datasource.postgres.model.util.userFirstNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userLastNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userTable
import com.laucoin.registry.core.datasource.postgres.model.util.visibleField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileAcceptedField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileEndAccessField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileRoleField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileStartAccessField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileTable

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

const val onlyVisibleClause = "(:onlyVisible IS FALSE OR t.$visibleField IS TRUE)"
