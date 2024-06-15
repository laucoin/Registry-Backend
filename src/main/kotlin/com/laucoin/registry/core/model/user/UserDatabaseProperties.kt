package com.laucoin.registry.core.model.user

import com.laucoin.registry.core.model.profile.profileAcceptedField
import com.laucoin.registry.core.model.profile.profileEndAccessField
import com.laucoin.registry.core.model.profile.profileRoleField
import com.laucoin.registry.core.model.profile.profileStartAccessField
import com.laucoin.registry.core.repository.util.eventIdField
import com.laucoin.registry.core.repository.util.idField
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
import com.laucoin.registry.core.repository.util.visibleField

const val defaultProfile = "default_profile_"

const val userTable = "tb_user"

const val oidcIdField = "oidc_id"
const val userFirstNameField = "first_name"
const val userLastNameField = "last_name"
const val userEmailField = "email"
const val userDefaultProfileIdField = defaultProfile + idField
const val userDefaultProfileRoleField = defaultProfile + profileRoleField
const val userDefaultProfileAcceptedField = defaultProfile + profileAcceptedField
const val userDefaultProfileStartAccessField = defaultProfile + profileStartAccessField
const val userDefaultProfileEndAccessField = defaultProfile + profileEndAccessField
const val userDefaultProfileVisibleField = defaultProfile + visibleField
const val userDefaultProfileEventIdField = defaultProfile + eventIdField
const val userDefaultProfileEventNameField = defaultProfile + linkedEventNameField
const val userDefaultProfileEventAddressIdField = defaultProfile + linkedEventAddressIdField
const val userDefaultProfileEventAddressNumberField = defaultProfile + linkedEventAddressNumberField
const val userDefaultProfileEventAddressStreetField = defaultProfile + linkedEventAddressStreetField
const val userDefaultProfileEventAddressComplementaryInformationField =
    defaultProfile + linkedEventAddressComplementaryInformationField
const val userDefaultProfileEventAddressZipCodeField = defaultProfile + linkedEventAddressZipCodeField
const val userDefaultProfileEventAddressCityField = defaultProfile + linkedEventAddressCityField
const val userDefaultProfileEventAddressCountryField = defaultProfile + linkedEventAddressCountryField
const val userDefaultProfileEventOptionsField = defaultProfile + linkedEventOptionsField
const val userDefaultProfileEventStartTimeField = defaultProfile + linkedEventStartTimeField
const val userDefaultProfileEventEndTimeField = defaultProfile + linkedEventEndTimeField
