package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.STATISTICS_MAN_SECONDS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.STATISTICS_MOVEMENT_SECONDS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.STATISTICS_PARTICIPANT_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.STATISTICS_PROJECT_PARTICIPANT_COUNT
import org.springframework.data.relational.core.mapping.Column

data class ActivityStatisticsEntity(
	@Column(STATISTICS_PARTICIPANT_COUNT)
	var participantCount: Long? = null,
	@Column(STATISTICS_PROJECT_PARTICIPANT_COUNT)
	var projectParticipantCount: Long? = null,
	@Column(STATISTICS_MOVEMENT_SECONDS)
	var movementSeconds: Double? = null,
	@Column(STATISTICS_MAN_SECONDS)
	var manSeconds: Double? = null,
)
