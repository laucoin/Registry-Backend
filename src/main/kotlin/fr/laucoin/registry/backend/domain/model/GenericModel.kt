package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.Objects
import java.util.UUID

abstract class GenericModel(
    var id: UUID? = null,
    var visible: Boolean = true,

    var creation: HistoryModel? = null,
    var lastEdition: HistoryModel? = null,
) {
    fun isNotVisible() = ! visible

    fun create(currentUser: CurrentUserModel, dateTime: ZonedDateTime = now()) {
        if (Objects.isNull(creation)) {
            creation = HistoryModel()
        }
        creation !!.user = HistoryUserModel(
            id = currentUser.id,
            firstName = currentUser.firstName,
            lastName = currentUser.lastName,
            email = currentUser.email,
        )
        creation !!.dateTime = dateTime
        this.update(currentUser, dateTime)
    }

    fun update(currentUser: CurrentUserModel, dateTime: ZonedDateTime = now()) {
        if (Objects.isNull(lastEdition)) {
            lastEdition = HistoryModel()
        }
        lastEdition !!.user = HistoryUserModel(
            id = currentUser.id,
            firstName = currentUser.firstName,
            lastName = currentUser.lastName,
            email = currentUser.email,
        )
        lastEdition !!.dateTime = dateTime
    }
}
