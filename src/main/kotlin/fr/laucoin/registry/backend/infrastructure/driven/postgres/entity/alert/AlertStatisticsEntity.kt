package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_AVERAGE_RESOLUTION_SECONDS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_CANCELED_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_RESOLVED_COUNT
import org.springframework.data.relational.core.mapping.Column

data class AlertStatisticsEntity(
	@Column(ALERT_RESOLVED_COUNT)
	var resolvedCount: Long? = null,
	@Column(ALERT_CANCELED_COUNT)
	var canceledCount: Long? = null,
	@Column(ALERT_AVERAGE_RESOLUTION_SECONDS)
	var averageResolutionSeconds: Double? = null,
)
