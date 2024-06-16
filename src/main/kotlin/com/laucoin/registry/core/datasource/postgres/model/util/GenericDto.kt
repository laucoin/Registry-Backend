package com.laucoin.registry.core.datasource.postgres.model.util

import com.laucoin.registry.core.model.util.GenericModel
import com.laucoin.registry.core.model.util.HistoryModel
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

open class GenericDto(
    @Id
    var id: UUID? = null,
    var visible: Boolean = true,
    @Column(createDateField)
    var creationDate: LocalDateTime? = null,
    @Column(createUserIdField)
    var creatorId: UUID? = null,
    @ReadOnlyProperty
    @Column(createUserFirstNameField)
    var creatorFirstName: String? = null,
    @ReadOnlyProperty
    @Column(createUserLastNameField)
    var creatorLastName: String? = null,
    @ReadOnlyProperty
    @Column(createUserEmailField)
    var creatorEmail: String? = null,
    @ReadOnlyProperty
    @Column(createUserVisibleField)
    var creatorVisible: Boolean? = null,
    @Column(editDateField)
    var editionDate: LocalDateTime? = null,
    @Column(editUserIdField)
    var editorId: UUID? = null,
    @ReadOnlyProperty
    @Column(editUserFirstNameField)
    var editorFirstName: String? = null,
    @ReadOnlyProperty
    @Column(editUserLastNameField)
    var editorLastName: String? = null,
    @ReadOnlyProperty
    @Column(editUserEmailField)
    var editorEmail: String? = null,
    @ReadOnlyProperty
    @Column(editUserVisibleField)
    var editorVisible: Boolean? = null,
) {
    open fun <T: GenericModel> populateGenericDto(element: T) {
        id = element.id
        visible = element.visible

        creationDate = element.creation?.date
        creatorId = element.creation?.user?.id

        editionDate = element.edition?.date
        editorId = element.edition?.user?.id
    }

    open fun <T: GenericModel> populateGenericModel(element: T): T {
        element.id = id
        element.visible = visible

        if (Objects.isNull(element.creation)) {
            element.creation = HistoryModel()
        }
        element.creation !!.date = creationDate
        element.creation !!.user.id = creatorId
        element.creation !!.user.firstName = creatorFirstName
        element.creation !!.user.lastName = creatorLastName
        element.creation !!.user.email = creatorEmail
        element.creation !!.user.visible = creatorVisible

        if (Objects.isNull(element.edition)) {
            element.edition = HistoryModel()
        }
        element.edition !!.date = editionDate
        element.edition !!.user.id = editorId
        element.edition !!.user.firstName = editorFirstName
        element.edition !!.user.lastName = editorLastName
        element.edition !!.user.email = editorEmail
        element.edition !!.user.visible = editorVisible

        return element
    }
}
