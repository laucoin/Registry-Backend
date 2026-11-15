package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.MovementEventModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEventEntity
import org.mapstruct.Mapper

@Mapper(config = PostgresMapperConfig::class)
interface MovementEventEntityMapper {
	fun toModel(entity: MovementEventEntity): MovementEventModel
}
