package com.laucoin.registry.domain.profile.datasource.postgres.model.util

import com.laucoin.registry.core.datasource.postgres.model.util.user
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userEmailField
import com.laucoin.registry.core.datasource.postgres.model.util.userFirstNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userLastNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userOidcIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userRoleField
import com.laucoin.registry.core.datasource.postgres.model.util.visibleField

const val profileTable = "tb_profile"

const val profileUserIdField = userIdField
const val profileUserOidcIdField = user + userOidcIdField
const val profileUserFirstNameField = user + userFirstNameField
const val profileUserLastNameField = user + userLastNameField
const val profileUserEmailField = user + userEmailField
const val profileUserRoleField = user + userRoleField
const val profileUserDefaultProfileIdField = user + userDefaultProfileIdField
const val profileUserVisibleField = user + visibleField
const val profileRoleField = "role"
const val profileAcceptedField = "accepted"
const val profileStartAccessField = "start_access"
const val profileEndAccessField = "end_access"
