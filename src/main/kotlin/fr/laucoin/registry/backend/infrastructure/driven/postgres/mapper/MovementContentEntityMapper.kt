package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.MovementContentMappingHelper
import java.util.UUID
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class, uses = [MovementContentMappingHelper::class])
interface MovementContentEntityMapper: IEntityReaderMapper<MovementContentModel, MovementContentEntity> {
	@Mapping(target = "participant", source = "entity", qualifiedByName = ["participantOf"])
	@Mapping(target = "vehicle", source = "entity", qualifiedByName = ["vehicleOf"])
	override fun toModel(entity: MovementContentEntity): MovementContentModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "movementId", source = "movementId")
	@Mapping(target = "poolName", source = "model.poolName")
	@Mapping(target = "participantId", source = "model.participant.id")
	@Mapping(target = "vehicleId", source = "model.vehicle.id")
	fun toEntity(movementId: UUID, model: MovementContentModel): MovementContentEntity
}
