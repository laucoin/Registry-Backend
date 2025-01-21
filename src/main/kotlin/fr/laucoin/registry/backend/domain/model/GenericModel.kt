package fr.laucoin.registry.backend.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.UUID

@JsonInclude(NON_NULL)
abstract class GenericModel(
    var id: UUID? = null,
    var visible: Boolean = true,

    var creation: HistoryModel = HistoryModel(),
    var lastEdition: HistoryModel = HistoryModel(),
) {
    @JsonIgnore
    abstract fun getSearchableValues(): List<String>

    @JsonIgnore
    fun isNotVisible() = ! visible

    fun create(currentUser: UserModel, dateTime: ZonedDateTime = now()) {
        creation.user = HistoryUserModel(
            id = currentUser.id,
            firstName = currentUser.firstName,
            lastName = currentUser.lastName,
            email = currentUser.email,
        )
        creation.dateTime = dateTime
        this.update(currentUser, dateTime)
    }

    fun update(currentUser: UserModel, dateTime: ZonedDateTime = now()) {
        lastEdition.user = HistoryUserModel(
            id = currentUser.id,
            firstName = currentUser.firstName,
            lastName = currentUser.lastName,
            email = currentUser.email,
        )
        lastEdition.dateTime = dateTime
    }
}
