package com.laucoin.registry.core.model.user

import java.util.UUID

data class HistoryUserModel(
    var id: UUID? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var visible: Boolean? = null,
)
