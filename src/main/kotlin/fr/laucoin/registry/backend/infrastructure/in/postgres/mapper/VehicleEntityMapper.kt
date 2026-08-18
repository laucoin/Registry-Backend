package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildAvailabilityWarning
import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithProjectAndModel
import org.springframework.stereotype.Component

@Component
class VehicleEntityMapper : IEntityMapper<VehicleModel, VehicleEntity> {
	override fun toModel(entity: VehicleEntity): VehicleModel {
		return VehicleModel().apply {
			licensePlate = entity.licensePlate
			brand = entity.brand
			model = entity.model
			startAvailability = mapCustomDateTime(entity.startAvailabilityDate, entity.startAvailabilityTime)
			endAvailability = mapCustomDateTime(entity.endAvailabilityDate, entity.endAvailabilityTime)
			status = buildStatus(entity.lastMovementType)
			availabilityWarning = buildAvailabilityWarning(entity.lastMovementType)
			lastMovement = entity.lastMovementDateTime
		}.fillWithProjectAndEntity(entity)
	}

	override fun toEntity(model: VehicleModel): VehicleEntity {
		return VehicleEntity().apply {
			licensePlate = model.licensePlate
			brand = model.brand
			this.model = model.model
			startAvailabilityDate = model.startAvailability?.date
			startAvailabilityTime = model.startAvailability?.time
			endAvailabilityDate = model.endAvailability?.date
			endAvailabilityTime = model.endAvailability?.time
		}.fillWithProjectAndModel(model)
	}
}
