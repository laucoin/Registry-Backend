package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.constant.DateTimeConst.EPOCH_UTC
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithProjectAndModel
import org.springframework.stereotype.Component

@Component
class AlertEntityMapper: IEntityMapper<AlertModel, AlertEntity> {
	override fun toModel(entity: AlertEntity): AlertModel {
		return AlertModel().apply {
			title = entity.title
			dateTime = entity.dateTime ?: EPOCH_UTC
			status = entity.status
			criticality = entity.criticality
			lastCommunication = entity.lastCommunicationDateTime?.let { CommunicationModel(it, entity.lastCommunicationMessage) }
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
