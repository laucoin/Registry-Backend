package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.MovementEventModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEventEntity
import org.springframework.stereotype.Component

@Component
class MovementEventEntityMapper {
	fun toModel(entity: MovementEventEntity): MovementEventModel {
		return MovementEventModel(
			dateTime = entity.dateTime!!,
			type = entity.type!!,
			reason = entity.reason,
		)
	}
}
