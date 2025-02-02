package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndModel
import org.springframework.stereotype.Component

@Component
class VehicleEntityMapper: IEntityMapper<VehicleModel, VehicleEntity> {
    override fun toModel(entity: VehicleEntity): VehicleModel {
        return VehicleModel().apply {
            registration = entity.registration
            brand = entity.brand
            model = entity.model
            begin = entity.begin
            end = entity.end
        }.fillWithEventAndEntity(entity)
    }

    override fun toEntity(model: VehicleModel): VehicleEntity {
        return VehicleEntity().apply {
            registration = model.registration
            brand = model.brand
            this.model = model.model
            begin = model.begin
            end = model.end
        }.fillWithEventAndModel(model)
    }
}
