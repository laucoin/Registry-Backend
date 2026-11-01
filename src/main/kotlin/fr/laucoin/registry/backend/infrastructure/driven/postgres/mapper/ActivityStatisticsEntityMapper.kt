package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.ActivityStatisticsEntity
import kotlin.math.roundToLong
import org.springframework.stereotype.Component

@Component
class ActivityStatisticsEntityMapper {
	fun toModel(entity: ActivityStatisticsEntity): ActivityStatisticsModel {
		return ActivityStatisticsModel(
			participantCount = entity.participantCount ?: 0,
			projectParticipantCount = entity.projectParticipantCount ?: 0,
			movementSeconds = entity.movementSeconds?.roundToLong() ?: 0,
			manSeconds = entity.manSeconds?.roundToLong() ?: 0,
		)
	}
}
