package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GenericEventModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import java.util.Objects

fun <M: GenericEventModel, E: GenericEventEntity> M.fillWithEventAndEntity(entity: E): M {
    event = if (Objects.nonNull(entity.eventId)) EventModel().apply {
        id = entity.eventId
        name = entity.eventName
        begin = entity.eventStartTime
        end = entity.eventEndTime
        options = entity.eventOptions
    } else null

    fillWithEntity(entity)

    return this
}

fun <M: GenericModel, E: GenericEntity> M.fillWithEntity(entity: E): M {
    id = entity.id
    visible = entity.visible ?: visible

    creation = HistoryModel(
        dateTime = entity.createdAt,
        user = HistoryUserModel(
            id = entity.creatorId,
            firstName = entity.creatorFirstName,
            lastName = entity.creatorLastName,
            email = entity.creatorEmail
        )
    )

    lastEdition = HistoryModel(
        dateTime = entity.lastUpdateAt,
        user = HistoryUserModel(
            id = entity.lastEditorId,
            firstName = entity.lastEditorFirstName,
            lastName = entity.lastEditorLastName,
            email = entity.lastEditorEmail
        )
    )

    return this
}

fun <M: GenericEventModel, E: GenericEventEntity> E.fillWithEventAndModel(model: M): E {
    eventId = model.event?.id

    fillWithModel(model)

    return this
}

fun <M: GenericModel, E: GenericEntity> E.fillWithModel(model: M): E {
    id = model.id
    visible = model.visible

    createdAt = model.creation.dateTime
    creatorId = model.creation.user?.id

    lastUpdateAt = model.lastEdition.dateTime
    lastEditorId = model.lastEdition.user?.id

    return this
}
