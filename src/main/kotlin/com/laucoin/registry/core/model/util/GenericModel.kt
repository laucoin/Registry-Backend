package com.laucoin.registry.core.model.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

@JsonInclude(NON_NULL)
abstract class GenericModel(
    @JsonProperty(access = READ_ONLY)
    var id: UUID? = null,
    @JsonProperty(access = READ_ONLY)
    var visible: Boolean = true,
    var creation: HistoryModel? = null,
    var edition: HistoryModel? = null,
) {
    fun <T: UserModel> create(user: T, dateTime: LocalDateTime = now()) {
        creation = setHistory(user, dateTime)
        update(user, dateTime)
    }

    fun <T: UserModel> update(user: T, dateTime: LocalDateTime = now()) {
        edition = setHistory(user, dateTime)
    }

    fun fillHistoryWithServiceAccountIfNecessary(serviceAccount: EnrichedUserModel) {
        if (creation?.user?.id == serviceAccount.id) {
            creation?.user = HistoryUserModel(
                id = serviceAccount.id,
                firstName = serviceAccount.firstName,
                lastName = serviceAccount.lastName,
                email = serviceAccount.email,
                visible = true,
            )
        }
        if (edition?.user?.id == serviceAccount.id) {
            edition?.user = HistoryUserModel(
                id = serviceAccount.id,
                firstName = serviceAccount.firstName,
                lastName = serviceAccount.lastName,
                email = serviceAccount.email,
                visible = true,
            )
        }
    }

    private fun <T: UserModel> setHistory(user: T, dateTime: LocalDateTime): HistoryModel = HistoryModel(
        date = dateTime,
        user = HistoryUserModel(
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            visible = user.visible
        )
    )

    abstract fun filterFields(): List<String?>
}
