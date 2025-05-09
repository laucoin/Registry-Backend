package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class VehicleEntityMapper: IEntityMapper<VehicleModel, VehicleEntity> {
    override fun toModel(entity: VehicleEntity): VehicleModel {
        return VehicleModel().apply {
            licensePlate = entity.licensePlate
            brand = entity.brand
            model = entity.model
            startAvailability = if (Objects.isNull(entity.startAvailabilityDate)) null
            else CustomDateTimeModel(entity.startAvailabilityDate !!, entity.startAvailabilityTime)
            endAvailability = if (Objects.isNull(entity.endAvailabilityDate)) null
            else CustomDateTimeModel(entity.endAvailabilityDate !!, entity.endAvailabilityTime)
            status = buildStatus(entity.lastMovementType)
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
