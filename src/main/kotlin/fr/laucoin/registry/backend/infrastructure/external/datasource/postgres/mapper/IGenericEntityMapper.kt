package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEntity

interface IGenericEntityMapper<M: GenericModel, E: GenericEntity>: IEntityMapper<M, E> {
    fun M.fillWithEntity(entity: E): M {
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

    fun E.fillWithModel(model: M): E {
        id = model.id
        visible = model.visible

        createdAt = model.creation.dateTime
        creatorId = model.creation.user?.id

        lastUpdateAt = model.lastEdition.dateTime
        lastEditorId = model.lastEdition.user?.id

        return this
    }
}
