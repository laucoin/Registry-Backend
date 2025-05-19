package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.time.ZonedDateTime
import org.springframework.stereotype.Component

@Component
class AlertEntityMapper: IEntityMapper<AlertModel, AlertEntity> {
    override fun toModel(entity: AlertEntity): AlertModel {
        return AlertModel().apply {
            title = entity.title
            dateTime = entity.dateTime ?: ZonedDateTime.now()
            status = entity.status
        }.fillWithProjectAndEntity(entity)
    }

    override fun toEntity(model: AlertModel): AlertEntity {
        return AlertEntity().apply {
            title = model.title
            dateTime = model.dateTime
            status = model.status
        }.fillWithProjectAndModel(model)
    }
}
