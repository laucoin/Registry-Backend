package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_REASON
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import java.time.ZonedDateTime
import org.springframework.data.relational.core.mapping.Column

data class MovementEventEntity(
	@Column(MOVEMENT_DATE_TIME)
	var dateTime: ZonedDateTime? = null,
	@Column(MOVEMENT_TYPE)
	var type: MovementTypeEnum? = null,
	@Column(MOVEMENT_REASON)
	var reason: MovementReasonEnum? = null,
)
