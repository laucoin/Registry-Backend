package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.AlertStatisticsModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertStatisticsEntity
import org.springframework.stereotype.Component
import kotlin.math.roundToLong

@Component
class AlertStatisticsEntityMapper {
	fun toModel(entity: AlertStatisticsEntity): AlertStatisticsModel {
		return AlertStatisticsModel(
			resolvedCount = entity.resolvedCount ?: 0,
			canceledCount = entity.canceledCount ?: 0,
			averageResolutionSeconds = entity.averageResolutionSeconds?.roundToLong(),
		)
	}
}
