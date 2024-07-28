package com.laucoin.registry.domain.profile.datasource.postgres.model.util

import com.laucoin.registry.core.datasource.postgres.model.util.idField
import com.laucoin.registry.core.datasource.postgres.model.util.userDefaultProfileIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userEmailField
import com.laucoin.registry.core.datasource.postgres.model.util.userFirstNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userLastNameField
import com.laucoin.registry.core.datasource.postgres.model.util.userRoleField
import com.laucoin.registry.core.datasource.postgres.model.util.userTable
import com.laucoin.registry.core.datasource.postgres.model.util.visibleField


const val linkedUserTable = "user_tb"
const val selectUser =
    "$linkedUserTable.$userFirstNameField AS $profileUserFirstNameField, " +
    "$linkedUserTable.$userLastNameField AS $profileUserLastNameField, " +
    "$linkedUserTable.$userEmailField AS $profileUserEmailField, " +
    "$linkedUserTable.$userRoleField AS $profileUserRoleField, " +
    "$linkedUserTable.$userDefaultProfileIdField AS $profileUserDefaultProfileIdField, " +
    "$linkedUserTable.$visibleField AS $profileUserVisibleField"
const val userJoin = "INNER JOIN $userTable $linkedUserTable ON t.$userIdField = $linkedUserTable.$idField"
