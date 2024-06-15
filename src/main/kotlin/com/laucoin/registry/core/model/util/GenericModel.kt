package com.laucoin.registry.core.model.util

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
import com.laucoin.registry.core.model.user.HistoryUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.repository.util.createDateField
import com.laucoin.registry.core.repository.util.createUserEmailField
import com.laucoin.registry.core.repository.util.createUserFirstNameField
import com.laucoin.registry.core.repository.util.createUserIdField
import com.laucoin.registry.core.repository.util.createUserLastNameField
import com.laucoin.registry.core.repository.util.createUserVisibleField
import com.laucoin.registry.core.repository.util.editDateField
import com.laucoin.registry.core.repository.util.editUserEmailField
import com.laucoin.registry.core.repository.util.editUserFirstNameField
import com.laucoin.registry.core.repository.util.editUserIdField
import com.laucoin.registry.core.repository.util.editUserLastNameField
import com.laucoin.registry.core.repository.util.editUserVisibleField
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

@JsonInclude(NON_NULL)
open class GenericModel(
    @Id
    @JsonProperty(access = READ_ONLY)
    var id: UUID? = null,
    @JsonProperty(access = READ_ONLY)
    var visible: Boolean = true,
    @JsonIgnore
    @Column(createDateField)
    var creationDate: LocalDateTime? = null,
    @JsonIgnore
    @Column(createUserIdField)
    var creatorId: UUID? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(createUserFirstNameField)
    var creatorFirstName: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(createUserLastNameField)
    var creatorLastName: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(createUserEmailField)
    var creatorEmail: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(createUserVisibleField)
    var creatorVisible: Boolean? = null,
    @JsonIgnore
    @Column(editDateField)
    var editionDate: LocalDateTime? = null,
    @JsonIgnore
    @Column(editUserIdField)
    var editorId: UUID? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(editUserFirstNameField)
    var editorFirstName: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(editUserLastNameField)
    var editorLastName: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(editUserEmailField)
    var editorEmail: String? = null,
    @JsonIgnore
    @ReadOnlyProperty
    @Column(editUserVisibleField)
    var editorVisible: Boolean? = null,
) {
    fun <T: UserModel> create(user: T) {
        creationDate = now()
        creatorId = user.id
        creatorFirstName = user.firstName
        creatorLastName = user.lastName
        creatorEmail = user.email
        creatorVisible = user.visible
        update(user, creationDate !!)
    }

    fun <T: UserModel> update(user: T, dateTime: LocalDateTime = now()) {
        editionDate = dateTime
        editorId = user.id
        editorFirstName = user.firstName
        editorLastName = user.lastName
        editorEmail = user.email
        editorVisible = user.visible
    }

    @ReadOnlyProperty
    @JsonProperty
    fun creation(): HistoryModel {
        return HistoryModel(
            date = creationDate,
            user = HistoryUserModel(
                id = creatorId,
                firstName = creatorFirstName,
                lastName = creatorLastName,
                email = creatorEmail,
                visible = creatorVisible,
            )
        )
    }

    @ReadOnlyProperty
    @JsonProperty
    fun edition(): HistoryModel {
        return HistoryModel(
            date = editionDate,
            user = HistoryUserModel(
                id = editorId,
                firstName = editorFirstName,
                lastName = editorLastName,
                email = editorEmail,
                visible = editorVisible,
            )
        )
    }
}
