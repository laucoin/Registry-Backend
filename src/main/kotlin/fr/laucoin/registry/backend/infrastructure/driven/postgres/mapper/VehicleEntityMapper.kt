package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class)
interface VehicleEntityMapper: IEntityMapper<VehicleModel, VehicleEntity> {
	@IgnoreModelAudit
	@AvailabilityDatesToModel
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "lastMovement", source = "lastMovementDateTime")
	override fun toModel(entity: VehicleEntity): VehicleModel

	@IgnoreEntityAudit
	@AvailabilityDatesToEntity
	@Mapping(target = "lastMovementType", ignore = true)
	@Mapping(target = "lastMovementDateTime", ignore = true)
	override fun toEntity(model: VehicleModel): VehicleEntity
}
