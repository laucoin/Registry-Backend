package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GenericEventModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import java.util.Objects

fun <M: GenericEventModel, E: GenericEventEntity> M.fillWithEventAndEntity(entity: E): M {
    event = if (Objects.isNull(entity.eventName)) null
    else EventModel().apply {
        id = entity.eventId
        name = entity.eventName
        begin = if (Objects.isNull(entity.eventStartDate)) null
        else CustomDateTimeModel(entity.eventStartDate !!, entity.eventStartTime)
        end = if (Objects.isNull(entity.eventEndDate) && Objects.isNull(entity.eventEndTime)) null
        else CustomDateTimeModel(entity.eventEndDate !!, entity.eventEndTime)
        options = entity.eventOptions
    }

    fillWithEntity(entity)

    return this
}

fun <M: GenericModel, E: GenericEntity> M.fillWithEntity(entity: E): M {
    id = entity.id
    visible = entity.visible ?: visible

    creation = if (Objects.isNull(entity.createdAt)) null
    else HistoryModel(
        dateTime = entity.createdAt !!,
        user = if (Objects.nonNull(entity.creatorId)) HistoryUserModel(
            id = entity.creatorId,
            firstName = entity.creatorFirstName,
            lastName = entity.creatorLastName,
            email = entity.creatorEmail
        ) else null
    )

    lastEdition = if (Objects.isNull(entity.lastUpdateAt)) null
    else HistoryModel(
        dateTime = entity.lastUpdateAt !!,
        user = if (Objects.nonNull(entity.lastEditorId)) HistoryUserModel(
            id = entity.lastEditorId,
            firstName = entity.lastEditorFirstName,
            lastName = entity.lastEditorLastName,
            email = entity.lastEditorEmail
        ) else null
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

    createdAt = model.creation?.dateTime
    creatorId = model.creation?.user?.id

    lastUpdateAt = model.lastEdition?.dateTime
    lastEditorId = model.lastEdition?.user?.id

    return this
}
