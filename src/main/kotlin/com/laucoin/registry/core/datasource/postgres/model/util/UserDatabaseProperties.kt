package com.laucoin.registry.core.datasource.postgres.model.util

import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileAcceptedField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileEndAccessField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileRoleField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileStartAccessField

const val defaultProfile = "default_profile_"

const val userTable = "tb_user"

const val userOidcIdField = "oidc_id"
const val userFirstNameField = "first_name"
const val userLastNameField = "last_name"
const val userEmailField = "email"
const val userRoleField = "role"
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
