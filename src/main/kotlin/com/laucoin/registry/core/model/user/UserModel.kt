package com.laucoin.registry.core.model.user

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
import com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY
import com.laucoin.registry.core.model.util.GenericModel
import java.util.UUID

open class UserModel(
    @JsonProperty(access = READ_ONLY)
    var oidcId: UUID? = null,
    @JsonProperty(access = READ_ONLY)
    var firstName: String? = null,
    @JsonProperty(access = READ_ONLY)
    var lastName: String? = null,
    @JsonProperty(access = READ_ONLY)
    var email: String? = null,
    var role: String? = null,
    @JsonProperty(access = WRITE_ONLY)
    var defaultProfileId: UUID? = null,
): GenericModel() {
    fun personalDataChanged(
        newEmail: String,
        newFirstName: String?,
        newLastName: String?
    ): Boolean = email !== newEmail || firstName !== newFirstName || lastName !== newLastName

    override fun filterFields(): List<String?> = listOf(firstName, lastName, email)
}
